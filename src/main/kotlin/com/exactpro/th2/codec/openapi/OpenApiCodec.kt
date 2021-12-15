/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.th2.codec.openapi.http.HttpContainer
import com.exactpro.th2.codec.openapi.http.RequestContainer
import com.exactpro.th2.codec.openapi.http.ResponseContainer
import com.exactpro.th2.codec.openapi.throwable.DecodeException
import com.exactpro.th2.codec.openapi.throwable.EncodeException
import com.exactpro.th2.codec.openapi.utils.JsonSchemaTypes
import com.exactpro.th2.codec.openapi.utils.MessageFormat
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.getMethods
import com.exactpro.th2.codec.openapi.visitors.json.DecodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.visitors.json.DecodeJsonObjectVisitor
import com.exactpro.th2.codec.openapi.visitors.json.EncodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.visitors.json.EncodeJsonObjectVisitor
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.plusAssign
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import com.exactpro.th2.common.grpc.AnyMessage.KindCase.MESSAGE
import com.exactpro.th2.common.grpc.AnyMessage.KindCase.RAW_MESSAGE
import com.exactpro.th2.common.message.getList
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.messageType
import mu.KotlinLogging
import java.nio.charset.StandardCharsets.UTF_8

class OpenApiCodec(
    private val dictionary: OpenAPI,
    private val settings: OpenApiCodecSettings
) : IPipelineCodec {

    private val messagesToSchema = mutableMapOf<String, HttpContainer>()

    init {
        dictionary.paths.forEach { pathKey, pathsValue ->
            pathsValue.getMethods().forEach { (methodKey, methodValue) ->
                // Request
                methodValue.requestBody?.content?.forEach { (typeKey, typeValue) ->
                    val schema = dictionary.getEndPoint(typeValue.schema).apply {
                        format = typeKey
                    }

                    messagesToSchema[combineName(listOf(pathKey, methodKey, typeKey))] =
                        RequestContainer(pathKey, methodKey, methodValue.parameters, schema)
                } ?: run {
                    messagesToSchema[combineName(listOf(pathKey, methodKey))] =
                        RequestContainer(pathKey, methodKey, methodValue.parameters, null)
                }


                // Response
                methodValue.responses?.forEach { (responseKey, responseValue) ->
                    responseValue.content?.forEach { (typeKey, typeValue) ->
                        val schema = dictionary.getEndPoint(typeValue.schema).apply {
                            format = typeKey
                        }
                        messagesToSchema[combineName(listOf(pathKey, methodKey, responseKey, typeKey))] =
                            ResponseContainer(pathKey, methodKey, responseKey, schema)
                    } ?: run {
                        messagesToSchema[combineName(listOf(pathKey, methodKey, responseKey))] =
                            ResponseContainer(pathKey, methodKey, responseKey, null)
                    }
                }
            }
        }
        LOGGER.info { "Messages to encode: ${messagesToSchema.keys.joinToString(", ") { it }}" }
    }

    private fun combineName(steps: List<String>): String {
        return buildString {
            for (step in steps) {
                step.split("{", "}", "-", "/", "_").forEach { word ->
                    append(word.capitalize())
                }
            }
        }
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
            "Response" -> {
                val uri = requireNotNull(rawMessage.metadata.propertiesMap[URI_PROPERTY]) { "URI property in metadata of response is required" }
                val method = requireNotNull(rawMessage.metadata.propertiesMap[METHOD_PROPERTY]?.lowercase()) { "Method property in metadata of response is required" }
                val code = requireNotNull(message.getString(STATUS_CODE_FIELD)) { "Code status field required inside of http response message" }
                val type = message.getList(HEADERS_FIELD)?.first { it.messageValue.getString("name") == "Content-Type" }?.messageValue?.getString("value")
                    ?: "null"

                val messageSchema = dictionary.paths[uri]?.getMethods()?.get(method)?.responses?.get(code)?.content?.get(type)?.schema?.run {
                    dictionary.getEndPoint(this)
                }

                if (body.isNotEmpty()) {
                    checkNotNull(messageSchema) { "Message with path $uri, method $method, code $code and type $type wasn't found" }

                    val visitor = when (JsonSchemaTypes.getType(messageSchema.type)
                        ?: error("Unsupported type of json schema ${messageSchema.type}")) {
                        JsonSchemaTypes.ARRAY -> DecodeJsonArrayVisitor(body)
                        JsonSchemaTypes.OBJECT -> DecodeJsonObjectVisitor(body)
                    }
                    SchemaWriter(dictionary).runCatching {
                        traverse(visitor, messageSchema)
                    }.onFailure {
                        throw DecodeException(
                            "Cannot parse json body of response with path $uri, method $method, code $code and type $type",
                            it
                        )
                    }
                    builder += visitor.getResult()
                }

            }
            "Request" -> {
                val uri = requireNotNull(rawMessage.metadata.propertiesMap[URI_PROPERTY]) { "URI property in metadata of response is required" }
                val method = requireNotNull(rawMessage.metadata.propertiesMap[METHOD_PROPERTY]) { "Method property in metadata of response is required" }

            }
            else -> error("Unsupported message type: ${message.messageType}")
        }


        return builder.build()
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

            val container =
                checkNotNull(messagesToSchema[messageType]) { "There no message $messageType in dictionary" }

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
                when (MessageFormat.getFormat(messageSchema.format)
                    ?: error("Unsupported format of message ${messageSchema.format}")) {
                    MessageFormat.JSON -> {
                        val visitor = when (JsonSchemaTypes.getType(messageSchema.type)
                            ?: error("Unsupported type of json schema ${messageSchema.type}")) {
                            JsonSchemaTypes.ARRAY -> EncodeJsonArrayVisitor(parsedMessage)
                            JsonSchemaTypes.OBJECT -> EncodeJsonObjectVisitor(parsedMessage)
                        }
                        SchemaWriter(dictionary).runCatching {
                            traverse(visitor, messageSchema)
                        }.onFailure {
                            throw EncodeException("Cannot parse json body of response message $messageType", it)
                        }
                        rawMessage.body = ByteString.copyFrom(visitor.getResult().toByteArray())
                    }
                }
            }

            when (container) {
                is ResponseContainer -> {
                    rawMessage.metadataBuilder.apply {
                        putProperties(METHOD_PROPERTY, container.method)
                        putProperties(URI_PROPERTY, container.path)
                        putProperties(CODE_PROPERTY, container.code)
                    }
                }
                is RequestContainer -> {
                    rawMessage.metadataBuilder.apply {
                        putProperties(METHOD_PROPERTY, container.method)
                        putProperties(URI_PROPERTY, container.path)
                    }
                }
            }

            builder += rawMessage
        }

        return builder.build()
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        const val RESPONSE_MESSAGE = "Response"
        const val METHOD_PROPERTY = "method"
        const val URI_PROPERTY = "uri"
        const val CODE_PROPERTY = "code"
        const val STATUS_CODE_FIELD = "statusCode"
        const val HEADERS_FIELD = "headers"
        const val REASON_FIELD = "reason"
        private const val HTTP_PROTOCOL = "http"
    }
}