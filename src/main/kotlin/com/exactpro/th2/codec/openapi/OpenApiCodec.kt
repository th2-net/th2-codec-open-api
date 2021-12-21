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
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.getMethods
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.VisitorFactory
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.plusAssign
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
import mu.KotlinLogging

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
                val visitor = VisitorFactory.createEncodeVisitor(container.bodyFormat!!, messageSchema.type, parsedMessage)
                SchemaWriter.instance.traverse(visitor, messageSchema)
                rawMessage.body = visitor.getResult()
            }

            container.fillMetadata(rawMessage.metadataBuilder)
            builder += createHeaderMessage(container).apply {
                parentEventId = parsedMessage.parentEventId
            }
            builder += rawMessage
        }

        return builder.build()
    }

    override fun decode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList

        require(messages.size < 3) { "Message group must contain only 1 or 2 messages" }
        require(messages[0].kindCase == MESSAGE) { "Message must be a raw message" }
        val message = messages[0].message

        val rawMessage = if (messages.size == 2) {
            require(messages[1].kindCase == RAW_MESSAGE) { "Message must be a raw message" }
            messages[1].rawMessage
        } else null


        val body = rawMessage?.body

        val builder = MessageGroup.newBuilder()
        builder += message

        if (body != null) {
            val bodyFormat = message.getList(HEADERS_FIELD)?.first { it.messageValue.getString("name") == "Content-Type" }?.messageValue?.getString("value")
                ?: "null"
            val messageSchema = when (message.messageType) {
                RESPONSE_MESSAGE -> {
                    val uri = requireNotNull(rawMessage.metadata.propertiesMap[URI_PROPERTY]) { "URI property in metadata from response is required" }
                    val method = requireNotNull(rawMessage.metadata.propertiesMap[METHOD_PROPERTY]?.lowercase()) { "Method property in metadata from response is required" }
                    val code = requireNotNull(message.getString(STATUS_CODE_FIELD)) { "Code status field required inside of http response message" }

                    dictionary.paths[uri]?.getMethods()?.get(method)?.responses?.get(code)?.content?.get(bodyFormat)?.schema?.run {
                        dictionary.getEndPoint(this)
                    } ?: error { "Response schema with path $uri, method $method, code $code and type $bodyFormat wasn't found" }
                }
                REQUEST_MESSAGE -> {
                    val uri = requireNotNull(message.getString(URI_FIELD)) { "URI field in request is required" }
                    val method = requireNotNull(message.getString(METHOD_FIELD)) { "Method field in request is required" }

                    dictionary.paths[uri]?.getMethods()?.get(method)?.requestBody?.content?.get(bodyFormat)?.schema?.run {
                        dictionary.getEndPoint(this)
                    } ?: error { "Request schema with path $uri, method $method and type $bodyFormat wasn't found" }
                }
                else -> error("Unsupported message type: ${message.messageType}")
            }

            messageSchema.runCatching {
                val visitor = VisitorFactory.createDecodeVisitor(bodyFormat, messageSchema.type, body)
                SchemaWriter.instance.traverse(visitor, this)
                builder += visitor.getResult().apply {
                    parentEventId = rawMessage.parentEventId
                }
            }.onFailure {
                throw DecodeException(
                    "Cannot parse body of http message",
                    it
                )
            }
        }
        return builder.build()
    }

    private fun HttpContainer.fillMetadata(metadata: RawMessageMetadata.Builder) {
        when (this) {
            is ResponseContainer -> {
                metadata.apply {
                    putProperties(METHOD_PROPERTY, method)
                    putProperties(URI_PROPERTY, path)
                    putProperties(CODE_PROPERTY, code)
                }
            }
            is RequestContainer -> {
                metadata.apply {
                    putProperties(METHOD_PROPERTY, method)
                    putProperties(URI_PROPERTY, path)
                }

            }
        }
    }

    private fun createHeaderMessage(container: HttpContainer) : Message.Builder {
        return when (container) {
            is ResponseContainer -> {
                message(RESPONSE_MESSAGE).apply {
                    addField(CODE_FIELD, container.code)
                    container.bodyFormat?.let {
                        addField(HEADERS_FIELD, listOf(message().apply {
                            addField("name", "Content-Type")
                            addField("value", container.bodyFormat)
                        }))
                    }
                }
            }
            is RequestContainer -> {
                message(REQUEST_MESSAGE).apply {
                    addField(URI_FIELD, container.path)
                    addField(METHOD_FIELD, container.method)
                    container.bodyFormat?.let {
                        addField(HEADERS_FIELD, listOf(message().apply {
                            addField("name", CONTENT_TYPE_PROPERTY)
                            addField("value", container.bodyFormat)
                        }))
                    }
                }
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