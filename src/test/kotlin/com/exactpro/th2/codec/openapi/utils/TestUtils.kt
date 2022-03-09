package com.exactpro.th2.codec.openapi.utils/*
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

import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.common.assertEqualMessages
import com.exactpro.th2.common.assertString
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.EventID
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.getList
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.message.plusAssign
import com.exactpro.th2.common.message.sessionAlias
import com.google.protobuf.ByteString
import org.junit.jupiter.api.Assertions
import java.util.Locale
import kotlin.test.fail


const val REQUEST_MESSAGE_TYPE = "Request"
const val RESPONSE_MESSAGE_TYPE = "Response"
const val FORMAT_HEADER_NAME = "Content-Type"

fun getResourceAsText(path: String): String {
    return String.Companion::class.java.classLoader.getResource(path)?.readText() ?: fail("Resource [$path] is required")
}

fun OpenApiCodec.testDecode(path: String, method: String, code: String?, type: String?, bodyData: String? = null): Message? {
    val alias = "testalias"
    val group = MessageGroup.newBuilder()
    val headerParentID = EventID.newBuilder().apply {
        id = "123"
    }.build()
    val bodyParentID = EventID.newBuilder().apply {
        id = "321"
    }.build()
    val headerType = if (code == null) REQUEST_MESSAGE_TYPE else RESPONSE_MESSAGE_TYPE
    val header = message(headerType).apply {
        metadataBuilder.putProperties("testProperty", "testValue")
        sessionAlias = alias
        parentEventId = headerParentID
        when(headerType) {
            RESPONSE_MESSAGE_TYPE -> {
                code?.let {
                    addField(OpenApiCodec.STATUS_CODE_FIELD, code)
                }
                type?.let {
                    addField(OpenApiCodec.HEADERS_FIELD, listOf(message().apply {
                        addField("name", FORMAT_HEADER_NAME)
                        addField("value", it)
                    }))
                }
            }
            REQUEST_MESSAGE_TYPE -> {
                addField(OpenApiCodec.URI_FIELD, path)
                addField(OpenApiCodec.METHOD_FIELD, method)
                type?.let {
                    addField(OpenApiCodec.HEADERS_FIELD, listOf(message().apply {
                        addField("name", FORMAT_HEADER_NAME)
                        addField("value", it)
                    }))
                }
            }
        }
    }.build()
    group += header

    bodyData?.let {
        group += RawMessage.newBuilder().apply {
            sessionAlias = alias
            parentEventId = bodyParentID
            metadataBuilder.putProperties("testProperty", "testValue")
            when(headerType) {
                RESPONSE_MESSAGE_TYPE -> {
                    metadataBuilder.putProperties(OpenApiCodec.URI_PROPERTY, path)
                    metadataBuilder.putProperties(OpenApiCodec.METHOD_PROPERTY, method)
                    code?.let {
                        metadataBuilder.putProperties(OpenApiCodec.CODE_PROPERTY, it)
                    }
                }
                REQUEST_MESSAGE_TYPE -> {
                    metadataBuilder.putProperties(OpenApiCodec.URI_PROPERTY, path)
                    metadataBuilder.putProperties(OpenApiCodec.METHOD_PROPERTY, method)
                }
            }
            body = ByteString.copyFrom(bodyData.toByteArray())
        }.build()
    }
    val decodeResult = decode(group.build())
    Assertions.assertTrue(decodeResult.messagesList.size>0)
    Assertions.assertTrue(decodeResult.messagesList[0].hasMessage())
    assertEqualMessages(header, decodeResult.messagesList[0].message)
    Assertions.assertEquals(headerParentID, decodeResult.messagesList[0].message.parentEventId)  {"Decode process shouldn't lose parent event id inside of header"}

    decodeResult.messagesList[0].message.let { decodedHeader ->
        Assertions.assertEquals(alias, decodedHeader.sessionAlias)
        header.metadata.propertiesMap.forEach {
            Assertions.assertEquals(it.value, decodedHeader.metadata.propertiesMap[it.key]) {"Property ${it.key} must be the same as in input message"}
        }
    }

    if (bodyData != null) {
        Assertions.assertEquals(2, decodeResult.messagesList.size)
        Assertions.assertTrue(decodeResult.messagesList[1].hasMessage())
        decodeResult.messagesList[1].message.let { decodedBody ->
            Assertions.assertEquals(alias, decodedBody.sessionAlias)
            Assertions.assertEquals(bodyParentID, decodedBody.parentEventId) {"Decode process shouldn't lose parent event id inside of body"}

            group.messagesList[1]?.rawMessage?.let { oldBody ->
                oldBody.metadata.propertiesMap.forEach {
                    Assertions.assertEquals(it.value, decodedBody.metadata.propertiesMap[it.key]) {"Property ${it.key} must be the same as in input message"}
                }
            }

        }

        return decodeResult.messagesList[1].message
    }
    return null
}

fun OpenApiCodec.testEncode(path: String, method: String, code: String?, type: String?, protocol: String, fillMessage: (Message.Builder.() -> Unit)? = null): RawMessage? {
    val rawParentID = EventID.newBuilder().apply {
        id = "123"
    }.build()

    val messageType = combineName(path, method, code?:"", type?:"")
    val messageToEncode = message(messageType).apply {
        metadataBuilder.protocol = protocol
        metadataBuilder.putProperties("testProperty", "testValue")
        parentEventId = rawParentID
        if (fillMessage != null) {
            fillMessage()
        }

    }.build()

    val messageGroup = MessageGroup.newBuilder()
    messageGroup += messageToEncode

    val resultGroup =  encode(messageGroup.build())

    Assertions.assertTrue(resultGroup.messagesList[0].hasMessage())

    val header = resultGroup.messagesList[0].message

    val headerType = if (code == null) REQUEST_MESSAGE_TYPE else RESPONSE_MESSAGE_TYPE

    header.run {
        Assertions.assertEquals(rawParentID, parentEventId) {"Decode process shouldn't lose parent event id inside of header"}
        Assertions.assertEquals(headerType, this.messageType)
        messageToEncode.metadata.propertiesMap.forEach {
            Assertions.assertEquals(it.value, this.metadata.propertiesMap[it.key]) {"Property ${it.key} must be the same as in input message"}
        }
        when (headerType) {
            REQUEST_MESSAGE_TYPE -> {
                assertString(OpenApiCodec.URI_FIELD, path)
                assertString(OpenApiCodec.METHOD_FIELD, method)
                type?.let {
                    this.getList(OpenApiCodec.HEADERS_FIELD)!![0].messageValue.assertString("value", type)
                } ?: Assertions.assertNull(this.getList(OpenApiCodec.HEADERS_FIELD)?.find { it.messageValue.getString("name") == FORMAT_HEADER_NAME })
            }
            RESPONSE_MESSAGE_TYPE -> {
                assertString(OpenApiCodec.CODE_FIELD, code)
                type?.let {
                    this.getList(OpenApiCodec.HEADERS_FIELD)!![0].messageValue.assertString("value", type)
                } ?: Assertions.assertNull(this.getList(OpenApiCodec.HEADERS_FIELD)?.find { it.messageValue.getString("name") == FORMAT_HEADER_NAME })
            }
            else -> error("Wrong type of header message: $headerType")
        }
    }

    val body = resultGroup.messagesList.getOrNull(1)?.rawMessage

    if (fillMessage != null) {
        Assertions.assertNotNull(body) {"Encode must have body in result of filling incoming message"}
        Assertions.assertEquals(2, resultGroup.messagesList.size)
        Assertions.assertTrue(resultGroup.messagesList[1].hasRawMessage())

        body!!.let { rawMessage ->
            messageToEncode.metadata.propertiesMap.forEach {
                Assertions.assertEquals(it.value, body.metadata.propertiesMap[it.key]) {"Property ${it.key} must be the same as in input message"}
            }
            Assertions.assertEquals(rawParentID, rawMessage.parentEventId) {"Encode process shouldn't lose parent event id inside of body"}
            code?.let {
                Assertions.assertEquals(it, rawMessage.metadata.propertiesMap[OpenApiCodec.CODE_PROPERTY])
            }
            Assertions.assertEquals(method.lowercase(), rawMessage.metadata.propertiesMap[OpenApiCodec.METHOD_PROPERTY]?.lowercase())
            Assertions.assertEquals(path, rawMessage.metadata.propertiesMap[OpenApiCodec.URI_PROPERTY])
        }
    } else {
        Assertions.assertEquals(1, resultGroup.messagesList.size)
    }

    return body
}

private fun combineName(vararg steps : String): String {
    return buildString {
        for (step in steps) {
            step.split("{", "}", "-", "/", "_").forEach { word ->
                append(word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            }
        }
    }
}

fun OpenApiCodec.encode(message: Message) = encode(MessageGroup.newBuilder().addMessages(AnyMessage.newBuilder().setMessage(message).build()).build())
fun OpenApiCodec.encode(message: RawMessage) = encode(MessageGroup.newBuilder().addMessages(AnyMessage.newBuilder().setRawMessage(message).build()).build())