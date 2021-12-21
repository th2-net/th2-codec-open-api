/*
 * Copyright 2021-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.codec.openapi

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecFactory.Companion.PROTOCOL
import com.exactpro.th2.codec.openapi.schemacontainer.HttpContainer
import com.exactpro.th2.codec.openapi.schemacontainer.RequestContainer
import com.exactpro.th2.codec.openapi.schemacontainer.ResponseContainer
import com.exactpro.th2.codec.openapi.throwable.DecodeException
import com.exactpro.th2.codec.openapi.throwable.EncodeException
import com.exactpro.th2.codec.openapi.utils.JsonSchemaTypes
import com.exactpro.th2.codec.openapi.utils.MessageFormat
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.getMethods
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonObjectVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonObjectVisitor
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.plusAssign
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import com.exactpro.th2.common.grpc.AnyMessage.KindCase.MESSAGE
import com.exactpro.th2.common.grpc.AnyMessage.KindCase.RAW_MESSAGE
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessageMetadata
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.getList
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.messageType
import io.swagger.v3.oas.models.media.Schema
import mu.KotlinLogging
import java.nio.charset.StandardCharsets.UTF_8

class OpenApiCodec(
    private val dictionary: OpenAPI,
    private val settings: OpenApiCodecSettings
) : IPipelineCodec {

    private val messagesToSchema = dictionary.toMap()

    init {
        SchemaWriter.createInstance(dictionary)
    }

    override fun encode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList
        val builder = MessageGroup.newBuilder()

        for (message in messages) {
            if (message.kindCase != MESSAGE) {
                builder.addMessages(message)
                continue
            }

            if (message.message.metadata.run { protocol.isNotEmpty() && protocol != PROTOCOL }) {
                builder.addMessages(message)
                continue
            }

            val parsedMessage = message.message
            val metadata = parsedMessage.metadata
            val messageType = metadata.messageType

            val container = checkNotNull(messagesToSchema[messageType]) { "There no message $messageType in dictionary" }

            val rawMessage = RawMessage.newBuilder().apply {
                parentEventId = parsedMessage.parentEventId
                metadataBuilder.apply {
                    putAllProperties(metadata.propertiesMap)
                    this.id = metadata.id
                    this.timestamp = metadata.timestamp
                    this.protocol = PROTOCOL
                }
            }

            container.body?.let { messageSchema ->
                when (MessageFormat.getFormat(container.format!!)
                    ?: error("Unsupported format of message ${messageSchema.format}")) {
                    MessageFormat.JSON -> {
                        val visitor = when (JsonSchemaTypes.getType(messageSchema.type)
                            ?: error("Unsupported type of json schema ${messageSchema.type}")) {
                            JsonSchemaTypes.ARRAY -> EncodeJsonArrayVisitor(parsedMessage)
                            JsonSchemaTypes.OBJECT -> EncodeJsonObjectVisitor(parsedMessage)
                        }
                        SchemaWriter.instance.runCatching {
                            traverse(visitor, messageSchema)
                        }.onFailure {
                            throw EncodeException("Cannot parse json body of response message $messageType", it)
                        }
                        rawMessage.body = ByteString.copyFrom(visitor.getResult().toByteArray())
                    }
                }
            }

            container.fillMetadata(rawMessage.metadataBuilder)
            builder += createHeaderMessage(container)
            builder += rawMessage
        }

        return builder.build()
    }

    override fun decode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList

        require(messages.size == 2) { "Message group must contain only 2 messages" }
        require(messages[0].kindCase == MESSAGE) { "Message must be a raw message" }
        require(messages[1].kindCase == RAW_MESSAGE) { "Message must be a raw message" }
        val message = messages[0].message
        val rawMessage = messages[1].rawMessage

        val body = rawMessage.body.toByteArray().toString(UTF_8)

        val builder = MessageGroup.newBuilder()
        builder += message

        when (message.messageType) {
            RESPONSE_MESSAGE -> {
                val uri = requireNotNull(rawMessage.metadata.propertiesMap[URI_PROPERTY]) { "URI property in metadata of response is required" }
                val method = requireNotNull(rawMessage.metadata.propertiesMap[METHOD_PROPERTY]?.lowercase()) { "Method property in metadata of response is required" }
                val code = requireNotNull(message.getString(STATUS_CODE_FIELD)) { "Code status field required inside of http response message" }
                val type = message.getList(HEADERS_FIELD)?.first { it.messageValue.getString("name") == "Content-Type" }?.messageValue?.getString("value")
                    ?: "null"

                val messageSchema = dictionary.paths[uri]?.getMethods()?.get(method)?.responses?.get(code)?.content?.get(type)?.schema?.run {
                    dictionary.getEndPoint(this)
                }

                if (body.isNotEmpty()) {
                    checkNotNull(messageSchema) { "Response schema with path $uri, method $method, code $code and type $type wasn't found" }

                    messageSchema.runCatching {
                        builder += decodeSingle(body, type)
                    }.onFailure {
                        throw DecodeException(
                            "Cannot parse json body of response with path $uri, method $method, code $code and type $type",
                            it
                        )
                    }
                }

            }
            REQUEST_MESSAGE -> {
                val uri = requireNotNull(message.getString(URI_PROPERTY)) { "URI property in fields of request is required" }
                val method = requireNotNull(message.getString(METHOD_PROPERTY)) { "Method property in fields of request is required" }
                val type = message.getList(HEADERS_FIELD)?.first { it.messageValue.getString("name") == "Content-Type" }?.messageValue?.getString("value")
                    ?: "null"

                val messageSchema = dictionary.paths[uri]?.getMethods()?.get(method)?.requestBody?.content?.get(type)?.schema?.run {
                    dictionary.getEndPoint(this)
                }

                if (body.isNotEmpty()) {
                    checkNotNull(messageSchema) { "Request schema with path $uri, method $method and type $type wasn't found" }

                    messageSchema.runCatching {
                        builder += decodeSingle(body, type)
                    }.onFailure {
                        throw DecodeException(
                            "Cannot parse json body of request with path $uri, method $method and type $type",
                            it
                        )
                    }
                }
            }
            else -> error("Unsupported message type: ${message.messageType}")
        }


        return builder.build()
    }

    private fun Schema<*>.decodeSingle(body: String, format: String): Message {
        when (MessageFormat.getFormat(format) ?: error("Unsupported format of message $format")) {
            MessageFormat.JSON -> {
                val visitor = when (JsonSchemaTypes.getType(type)
                    ?: error("Unsupported type of json schema ${type}")) {
                    JsonSchemaTypes.ARRAY -> DecodeJsonArrayVisitor(body)
                    JsonSchemaTypes.OBJECT -> DecodeJsonObjectVisitor(body)
                }
                SchemaWriter.instance.traverse(visitor, this)
                return visitor.getResult()
            }
        }
    }

    private fun HttpContainer.fillMetadata(metadata: RawMessageMetadata.Builder) {
        metadata.apply {
            when (this@fillMetadata) {
                is ResponseContainer -> {
                    putProperties(METHOD_PROPERTY, method)
                    putProperties(URI_PROPERTY, path)
                    putProperties(CODE_PROPERTY, code)
                }
                is RequestContainer -> {
                    putProperties(METHOD_PROPERTY, method)
                    putProperties(URI_PROPERTY, path)
                }
            }
        }
    }

    private fun createHeaderMessage(container: HttpContainer) : Message {
        return when (container) {
            is ResponseContainer -> {
                message(RESPONSE_MESSAGE).apply {
                    addField(CODE_FIELD, container.code)
                    container.format?.let {
                        addField(HEADERS_FIELD, listOf(message().apply {
                            addField("name", "Content-Type")
                            addField("value", container.format)
                        }))
                    }
                }.build()
            }
            is RequestContainer -> {
                message(REQUEST_MESSAGE).apply {
                    addField(URI_FIELD, container.path)
                    addField(METHOD_FIELD, container.method)
                    container.format?.let {
                        addField(HEADERS_FIELD, listOf(message().apply {
                            addField("name", CONTENT_TYPE_PROPERTY)
                            addField("value", container.format)
                        }))
                    }
                }.build()
            }
        }
    }

    private fun OpenAPI.toMap(): Map<String, HttpContainer> {
        val map = mutableMapOf<String, HttpContainer>()
        dictionary.paths.forEach { pathKey, pathsValue ->
            pathsValue.getMethods().forEach { (methodKey, methodValue) ->
                // Request
                methodValue.requestBody?.content?.forEach { (typeKey, typeValue) ->
                    map[combineName(pathKey, methodKey, typeKey)] =
                        RequestContainer(pathKey, methodKey, methodValue.parameters, dictionary.getEndPoint(typeValue.schema), typeKey)
                } ?: run {
                    map[combineName(pathKey, methodKey)] =
                        RequestContainer(pathKey, methodKey, methodValue.parameters, null, null)
                }


                // Response
                methodValue.responses?.forEach { (responseKey, responseValue) ->
                    responseValue.content?.forEach { (typeKey, typeValue) ->
                        map[combineName(pathKey, methodKey, responseKey, typeKey)] =
                            ResponseContainer(pathKey, methodKey, responseKey, dictionary.getEndPoint(typeValue.schema), typeKey)
                    } ?: run {
                        map[combineName(pathKey, methodKey, responseKey)] =
                            ResponseContainer(pathKey, methodKey, responseKey, null, null)
                    }
                }
            }
        }
        LOGGER.info { "Schemas to map: ${map.keys.joinToString(", ") { it }}" }
        return map
    }

    private fun combineName(vararg steps: String): String {
        return buildString {
            for (step in steps) {
                step.split("{", "}", "-", "/", "_").forEach { word ->
                    append(word.capitalize())
                }
            }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        const val RESPONSE_MESSAGE = "Response"
        const val REQUEST_MESSAGE = "Request"
        const val METHOD_PROPERTY = "method"
        const val METHOD_FIELD = METHOD_PROPERTY
        const val CONTENT_TYPE_PROPERTY = "Content-Type"
        const val URI_PROPERTY = "uri"
        const val URI_FIELD = "uri"
        const val CODE_PROPERTY = "code"
        const val CODE_FIELD = CODE_PROPERTY
        const val STATUS_CODE_FIELD = "statusCode"
        const val HEADERS_FIELD = "headers"
    }
}