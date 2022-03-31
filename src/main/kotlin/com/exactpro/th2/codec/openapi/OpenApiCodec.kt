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
import com.exactpro.th2.codec.openapi.OpenApiCodecFactory.Companion.PROTOCOLS
import com.exactpro.th2.codec.openapi.schemacontainer.HttpRouteContainer
import com.exactpro.th2.codec.openapi.schemacontainer.RequestContainer
import com.exactpro.th2.codec.openapi.schemacontainer.ResponseContainer
import com.exactpro.th2.codec.openapi.throwable.DecodeException
import com.exactpro.th2.codec.openapi.throwable.EncodeException
import com.exactpro.th2.codec.openapi.utils.containingFormatOrNull
import com.exactpro.th2.codec.openapi.utils.extractType
import com.exactpro.th2.codec.openapi.utils.getByMethod
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.getMethods
import com.exactpro.th2.codec.openapi.utils.validateForType
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
import com.exactpro.th2.common.message.get
import com.exactpro.th2.common.message.getList
import com.exactpro.th2.common.message.getMessage
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.message.orEmpty
import com.exactpro.th2.common.message.sessionAlias
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.parameters.Parameter
import mu.KotlinLogging

class OpenApiCodec(private val dictionary: OpenAPI, settings: OpenApiCodecSettings) : IPipelineCodec {

    private val typeToSchema: Map<String, HttpRouteContainer>
    private val patternToPathItem: List<Pair<UriPattern, PathItem>>
    private val schemaWriter = SchemaWriter(dictionary, settings.checkUndefinedFields)

    init {
        val mapForName = mutableMapOf<String, HttpRouteContainer>()
        val mapForPatterns = mutableMapOf<UriPattern, PathItem>()
        dictionary.paths.forEach { pathKey, pathsValue ->
            val pattern = UriPattern(pathKey)
            mapForPatterns[pattern] = pathsValue

            val pathParams = mutableMapOf<String, Parameter>().apply {
                pathsValue.parameters?.associateByTo(this, Parameter::getName)
            }

            pathsValue.getMethods().forEach { (methodKey, methodValue) ->

                val resultParams = pathParams.apply {
                    methodValue.parameters?.associateByTo(this, Parameter::getName)
                }


                // Request
                methodValue.requestBody?.content?.forEach { (typeKey, typeValue) ->
                    mapForName[combineName(pathKey, methodKey, typeKey)] = RequestContainer(
                        pattern, methodKey, dictionary.getEndPoint(typeValue.schema).validateForType(), typeKey, resultParams
                    )

                    if (methodValue.requestBody.required /*nullable*/ == false) {
                        mapForName[combineName(pathKey, methodKey)] = RequestContainer(pattern, methodKey, null, null, resultParams)
                    }
                } ?: run {
                    mapForName[combineName(pathKey, methodKey)] = RequestContainer(pattern, methodKey, null, null, resultParams)
                }

                // Response
                methodValue.responses?.forEach { (responseKey, responseValue) ->
                    responseValue.content?.forEach { (typeKey, typeValue) ->
                        mapForName[combineName(pathKey, methodKey, responseKey, typeKey)] = ResponseContainer(
                            pattern, methodKey, responseKey, dictionary.getEndPoint(typeValue.schema).validateForType(), typeKey, resultParams
                        )
                    } ?: run {
                        mapForName[combineName(pathKey, methodKey, responseKey)] = ResponseContainer(pattern, methodKey, responseKey, null, null, resultParams)
                    }
                }
            }
        }
        LOGGER.info { "Schemas to map: ${mapForName.keys.joinToString(", ")}" }
        typeToSchema = mapForName
        patternToPathItem = mapForPatterns.toList()
    }

    override fun encode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList
        val builder = MessageGroup.newBuilder()

        for (message in messages) {
            if (!message.hasMessage()) {
                builder.addMessages(message)
                continue
            }

            val parsedMessage = message.message
            val metadata = parsedMessage.metadata
            val messageType = metadata.messageType

            if(messageType == REQUEST_MESSAGE || messageType == RESPONSE_MESSAGE || metadata.protocol.run { isNotEmpty() && this !in PROTOCOLS }) {
                builder.addMessages(message)
                continue
            }

            val container = checkNotNull(typeToSchema[messageType]) { "There no message $messageType in dictionary" }

            builder += createHeaderMessage(container, parsedMessage).apply {
                if (parsedMessage.hasParentEventId()) parentEventId = parsedMessage.parentEventId
                sessionAlias = parsedMessage.sessionAlias
                metadataBuilder.putAllProperties(parsedMessage.metadata.propertiesMap)
                LOGGER.trace { "Created header message for ${parsedMessage.messageType}: ${this.messageType}" }
            }

            try {
                encodeBody(container, parsedMessage)?.let(builder::plusAssign)
            } catch (e: Exception) {
                throw EncodeException("Cannot encode body of message [${parsedMessage.messageType}]", e)
            }

        }

        return builder.build()
    }

    private fun encodeBody(container: HttpRouteContainer, message: Message): RawMessage? {
        val messageSchema = container.body ?: run {
            LOGGER.debug { "Body wasn't found for message: ${message.messageType}" }
            return null
        }

        LOGGER.debug { "Start of message encoding: ${message.messageType}" }
        checkNotNull(messageSchema.type) {"Type of schema [${messageSchema.name}] wasn't filled"}

        val visitor = VisitorFactory.createEncodeVisitor(container.bodyFormat!!, messageSchema.type, message)
        schemaWriter.traverse(visitor, messageSchema)
        val result = visitor.getResult()

        LOGGER.trace { "Result of encoded message ${message.messageType}: $result" }

        return if (!result.isEmpty) {
            RawMessage.newBuilder().apply {
                parentEventId = message.parentEventId
                sessionAlias = message.sessionAlias
                container.fillHttpMetadata(metadataBuilder)
                metadataBuilder.apply {
                    putAllProperties(message.metadata.propertiesMap)
                    this.id = metadata.id
                    this.timestamp = metadata.timestamp
                    protocol = message.metadata.protocol
                }
                body = result
            }.build()
        } else null
    }

    override fun decode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList

        require(messages.size < 3) { "Message group must contain only 1 or 2 messages" }
        require(messages[0].kindCase == MESSAGE) { "Message must be a raw message" }
        val builder = MessageGroup.newBuilder()

        val message = messages[0].message
        builder += message

        if (messages.size == 2) {
            require(messages[1].kindCase == RAW_MESSAGE) { "Message must be a raw message" }
            builder += runCatching {
                decodeBody(message, messages[1].rawMessage!!)
            }.getOrElse {
                throw DecodeException("Cannot parse body of http message", it)
            }
        }

        return builder.build()
    }

    private fun decodeBody(header: Message, rawMessage: RawMessage): Message {
        val body = rawMessage.body
        val (messageType, container) = searchContainer(header, rawMessage.metadata)
        val schema = checkNotNull(container.body) { "Container: $messageType did not contain schema body" }
        val format = checkNotNull(container.bodyFormat) { "Container: $messageType did not contain schema bodyFormat" }

        val visitor = VisitorFactory.createDecodeVisitor(format, schema.type, body)
        schemaWriter.traverse(visitor, schema)
        return visitor.getResult().apply {
            if(rawMessage.hasParentEventId()) parentEventId = rawMessage.parentEventId
            sessionAlias = rawMessage.sessionAlias
            this.messageType = messageType
            metadataBuilder.apply {
                id = rawMessage.metadata.id
                timestamp = metadata.timestamp
                protocol = rawMessage.metadata.protocol
                putAllProperties(rawMessage.metadata.propertiesMap)
            }
        }.build()
    }

    private fun searchContainer(header: Message, rawMetadata: RawMessageMetadata): Pair<String, HttpRouteContainer> {
        val headerFormat = header.getList(HEADERS_FIELD)
            ?.first { it.messageValue.getString("name") == "Content-Type" }
            ?.messageValue
            ?.getString("value")
            ?.extractType() ?: "null"

        val method: String
        val schemaFormat: String
        var code = ""

        val messageType: String

        val patternToPathItem: Pair<UriPattern, PathItem>

        when (header.messageType) {
            RESPONSE_MESSAGE -> {
                val uri = requireNotNull(rawMetadata.propertiesMap[URI_PROPERTY]) { "URI property in metadata from response is required" }
                method = requireNotNull(rawMetadata.propertiesMap[METHOD_PROPERTY]?.lowercase()) { "Method property in metadata from response is required" }
                code = requireNotNull(header.getString(STATUS_CODE_FIELD)) { "Code status field required inside of http response message" }

                patternToPathItem = checkNotNull(this.patternToPathItem.firstOrNull { it.first.matches(uri) }) { "Cannot find path-item by uri: $uri" }
                val content = patternToPathItem.second.getByMethod(method)?.responses?.get(code)?.content
                schemaFormat = checkNotNull(content?.containingFormatOrNull(headerFormat)) {
                    "Schema Response [${patternToPathItem.first.pattern}] with method: [$method] and code: [$code] did not contain type $headerFormat"
                }
                messageType = combineName(patternToPathItem.first.pattern, method, code, schemaFormat)
            }
            REQUEST_MESSAGE -> {
                val uri = requireNotNull(header.getString(URI_FIELD)) { "URI field in request is required" }
                method = requireNotNull(header.getString(METHOD_FIELD)) { "Method field in request is required" }

                patternToPathItem = checkNotNull(this.patternToPathItem.firstOrNull { it.first.matches(uri) }) { "Cannot find path-item by uri: $uri" }
                val content = patternToPathItem.second.getByMethod(method)?.requestBody?.content
                schemaFormat = checkNotNull(content?.containingFormatOrNull(headerFormat)) {
                    "Schema Response [${patternToPathItem.first.pattern}] with method: [$method] did not contain type $headerFormat"
                }
                messageType = combineName(patternToPathItem.first.pattern, method, code, schemaFormat)
            }
            else -> error("Unsupported header message type: ${header.messageType}")
        }

        val container = checkNotNull(typeToSchema[messageType]) { "Container for path: [${patternToPathItem.first.pattern}] with method: [$method], code: [$code] and type [$schemaFormat] wasn't found" }

        LOGGER.debug { "Container for ${header.messageType} with messageType: $messageType was found" }

        return messageType to container
    }


    private fun HttpRouteContainer.fillHttpMetadata(metadata: RawMessageMetadata.Builder) {
        when (this) {
            is ResponseContainer -> metadata.apply {
                method?.let { putProperties(METHOD_PROPERTY, it.uppercase()) }
                putProperties(URI_PROPERTY, uriPattern.pattern)
                putProperties(CODE_PROPERTY, code)
            }
            is RequestContainer -> metadata.apply {
                method?.let { putProperties(METHOD_PROPERTY, it.uppercase()) }
                putProperties(URI_PROPERTY, uriPattern.pattern)
            }
        }
    }

    private fun createHeaderMessage(container: HttpRouteContainer, message: Message): Message.Builder {
        return when (container) {
            is ResponseContainer -> message(RESPONSE_MESSAGE).addField(CODE_FIELD, container.code)
            is RequestContainer -> {
                message(REQUEST_MESSAGE).apply {
                    if (container.params.isNotEmpty()) {
                        addField(URI_FIELD, container.uriPattern.resolve(container.params, message.getMessage(URI_PARAMS_FIELD).orEmpty().fieldsMap.mapValues { it.value.simpleValue }))
                    } else {
                        addField(URI_FIELD, container.uriPattern.pattern)
                    }
                    addField(METHOD_FIELD, container.method)
                }
            }
            else -> error("Wrong type of Http Route Container")
        }.apply {
            // create headers for both request and response
            val headers = mutableListOf<Message.Builder>()

            container.bodyFormat?.let {
                headers.add(message().apply {
                    addField("name", CONTENT_TYPE_PROPERTY)
                    addField("value", container.bodyFormat)
                })
            }

            if (container.headers.isNotEmpty()) {
                val headerMessage = message.getMessage(HEADER_PARAMS_FIELD).orEmpty()
                container.headers.forEach { (name, value) ->
                    headerMessage[name]?.let { header ->
                        headers.add(message().apply {
                            addField("name", name)
                            addField("value", header.simpleValue)
                        })
                    } ?: run { if (value.required) error("Header param [$name] is required for ${message.messageType} message") }
                }
            }

            addField(HEADERS_FIELD, headers)
            this.metadataBuilder.protocol = HEADER_PROTOCOL
        }
    }

    fun combineName(vararg steps: String) = steps.asSequence().flatMap { it.replace("*","Any").split(COMBINER_REGEX) }.joinToString("") { it.lowercase().capitalize() }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private val COMBINER_REGEX = """[^A-Za-z0-9]""".toRegex()
        const val RESPONSE_MESSAGE = "Response"
        const val REQUEST_MESSAGE = "Request"
        const val METHOD_PROPERTY = "method"
        const val METHOD_FIELD = METHOD_PROPERTY
        const val CONTENT_TYPE_PROPERTY = "Content-Type"
        const val URI_PROPERTY = "uri"
        const val URI_FIELD = URI_PROPERTY
        const val CODE_PROPERTY = "code"
        const val CODE_FIELD = CODE_PROPERTY
        const val STATUS_CODE_FIELD = "statusCode"
        const val HEADERS_FIELD = "headers"
        const val URI_PARAMS_FIELD = "uriParameters"
        const val HEADER_PARAMS_FIELD = "headerParameters"
        const val HEADER_PROTOCOL = "http"
    }
}
