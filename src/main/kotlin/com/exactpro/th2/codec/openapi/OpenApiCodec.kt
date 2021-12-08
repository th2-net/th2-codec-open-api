package com.exactpro.th2.codec.openapi

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecFactory.Companion.PROTOCOL
import com.exactpro.th2.codec.openapi.visitors.JsonVisitor
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.plusAssign
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.OpenAPI
import mu.KotlinLogging

class OpenApiCodec(
    private val dictionary: OpenAPI,
    private val settings: OpenApiCodecSettings
) : IPipelineCodec {

    private val messagesToSchema = mutableMapOf<String, Schema<*>>()

    init {
        dictionary.paths.forEach { pathKey, pathsValue ->
            pathsValue.getMethods().forEach { (methodKey, methodValue) ->
                // Request
                methodValue.requestBody?.content?.forEach { (typeKey, typeValue) ->
                    messagesToSchema[combineName(listOf(pathKey, methodKey, typeKey))] = typeValue.schema.apply {
                        format = typeKey
                    }
                }


                // Response
                methodValue.responses?.forEach { (responseKey, responseValue) ->
                    responseValue.content?.forEach { (typeKey, typeValue) ->

                        messagesToSchema[combineName(listOf(pathKey, methodKey, responseKey, typeKey))] = typeValue.schema.apply {
                            format = typeKey
                        }
                    }
                }
            }
        }
        LOGGER.info { "Messages to encode: ${messagesToSchema.keys.joinToString(", ") { it }}" }
    }

    private fun combineName(steps: List<String>) : String {
        return buildString {
            for (step in steps) {
                step.split("{", "}", "-", "/", "_").forEach { word ->
                    append(word.capitalize())
                }
            }
        }
    }

    override fun decode(messageGroup: MessageGroup): MessageGroup {
        TODO("Not yet implemented")
    }

    override fun encode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList
        val builder = MessageGroup.newBuilder()

        for (message in messages) {
            if (!message.hasMessage()) {
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

            val messageSchema = checkNotNull(messagesToSchema[messageType]) {"There no message $messageType in dictionary"}

            when (MessageFormat.getFormat(messageSchema.format) ?: error("Unsupported format of message ${messageSchema.format}")) {
                MessageFormat.JSON -> {
                    val visitor = JsonVisitor()
                    SchemaWriter(dictionary).traverse(visitor, messageSchema, parsedMessage)
                    builder += RawMessage.newBuilder().apply {
                        body = ByteString.copyFrom(visitor.rootNode.toPrettyString().toByteArray())
                        parentEventId = parsedMessage.parentEventId
                        metadataBuilder.apply {
                            putAllProperties(metadata.propertiesMap)
                            this.id = metadata.id
                            this.timestamp = metadata.timestamp
                            this.protocol = PROTOCOL
                        }
                    }
                }
            }
        }

        return builder.build()
    }



    companion object {
        private val LOGGER = KotlinLogging.logger { }
        const val RESPONSE_MESSAGE = "Response"
        const val METHOD_FIELD = "method"
        const val URI_FIELD = "uri"
        const val STATUS_CODE_FIELD = "statusCode"
        const val REASON_FIELD = "reason"
        private const val HTTP_PROTOCOL = "http"
    }
}