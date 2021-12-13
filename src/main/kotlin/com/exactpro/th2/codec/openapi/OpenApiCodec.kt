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
import com.exactpro.th2.codec.openapi.utils.JsonSchemaTypes
import com.exactpro.th2.codec.openapi.utils.MessageFormat
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.getMethods
import com.exactpro.th2.codec.openapi.visitors.JsonArrayVisitor
import com.exactpro.th2.codec.openapi.visitors.JsonObjectVisitor
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
                            JsonSchemaTypes.ARRAY -> JsonArrayVisitor(parsedMessage)
                            JsonSchemaTypes.OBJECT -> JsonObjectVisitor(parsedMessage)
                        }
                        SchemaWriter(dictionary).traverse(visitor, messageSchema)
                        rawMessage.body = ByteString.copyFrom(visitor.getResult().toByteArray())
                    }
                }
            }

            when (container) {
                is ResponseContainer -> {
                    rawMessage.metadataBuilder.apply {
                        putProperties(METHOD_FIELD,  container.method)
                        putProperties(URI_FIELD, container.path)
                        putProperties(CODE_FIELD, container.code)
                    }
                }
                is RequestContainer -> {
                    rawMessage.metadataBuilder.apply {
                        putProperties(METHOD_FIELD,  container.method)
                        putProperties(URI_FIELD, container.path)
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
        const val METHOD_FIELD = "method"
        const val URI_FIELD = "uri"
        const val CODE_FIELD = "code"
        const val STATUS_CODE_FIELD = "statusCode"
        const val REASON_FIELD = "reason"
        private const val HTTP_PROTOCOL = "http"
    }
}