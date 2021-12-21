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

import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.common.assertEqualMessages
import com.exactpro.th2.common.assertString
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.getList
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.plusAssign
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path)!!.readText()
}

inline fun <reified T:Any>createTestSchema(value: T?, fillEnum: List<T>? = null) : Schema<*> {
    when (T::class) {
        String::class -> {
            return StringSchema().apply {
                type = "string"
                example = value
                fillEnum?.forEach {
                    enum.add(it.toString())
                }
            }
        }
        Boolean::class -> {
            return BooleanSchema().apply {
                type = "boolean"
                example = value
            }
        }
        Int::class -> {
            return NumberSchema().apply {
                type = "number"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Int).toBigDecimal())
                }
            }
        }
        Long::class -> {
            return NumberSchema().apply {
                type = "number"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Long).toBigDecimal())
                }
            }
        }
        Float::class -> {
            return NumberSchema().apply {
                type = "number"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Float).toBigDecimal())
                }
            }
        }
        Double::class -> {
            return NumberSchema().apply {
                type = "number"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Double).toBigDecimal())
                }
            }
        }
        else -> {
            throw UnsupportedOperationException("createTestSchema don't supports ${T::class} type of value")
        }
    }
}

fun createArrayTestSchema(type: String, format: String? = null) : ArraySchema {
    return ArraySchema().apply {
        items = StringSchema()
        items.type = type
        format?.let {
            items.format = format
        }
    }
}

fun JsonNode.asSchema() : Schema<*> {
    return when (this.nodeType) {
        JsonNodeType.ARRAY -> ArraySchema().apply {
            val arrayNode = this@asSchema as ArrayNode
            if (arrayNode.get(0).isObject) {
                this.items = arrayNode.map { it.asSchema() }.reduce { to, from  -> to.apply { properties = properties + from.properties } }
            } else {
                this.items = arrayNode.get(0).asSchema()
            }
        }
        JsonNodeType.OBJECT -> ObjectSchema().apply {
            properties = mutableMapOf()
            fields().forEach {
                properties[it.key] = it.value.asSchema()
            }
        }
        JsonNodeType.NUMBER -> {
            if (this.isFloat) {
                NumberSchema().apply {
                    format = "float"
                    example = this@asSchema.asInt()
                }
            } else if (this.isDouble) {
                NumberSchema().apply {
                    format = "double"
                    example = this@asSchema.asDouble()
                }
            } else if (this.isInt) {
                IntegerSchema().apply {
                    example = this@asSchema.asInt()
                }
            } else if (this.isLong) {
                IntegerSchema().apply {
                    type = "int64"
                    example = this@asSchema.asInt()
                }
            } else error("Wrong type of number inside json")
        }
        JsonNodeType.STRING -> {
            StringSchema().apply {
                example = this@asSchema.asText()
            }
        }
        JsonNodeType.BOOLEAN -> {
            BooleanSchema().apply {
                example = this@asSchema.asBoolean()
            }
        }
        JsonNodeType.NULL -> {
            StringSchema()
        }
        else -> {
            error("Wrong JsonNodeType of number inside json")
        }
    }
}

fun JsonNode.assertString(fieldName: String, fieldValue: String) {
    if (!get(fieldName).isTextual) {
        fail("$fieldName isn't textual type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asText())
}

fun JsonNode.assertBoolean(fieldName: String, fieldValue: Boolean) {
    if (!get(fieldName).isBoolean) {
        fail("$fieldName isn't boolean type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asBoolean())
}

fun JsonNode.assertFloat(fieldName: String, fieldValue: Float) {
    if (!get(fieldName).isNumber) {
        fail("$fieldName isn't float type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asText().toFloat())
}

fun JsonNode.assertDouble(fieldName: String, fieldValue: Double) {
    if (!get(fieldName).isNumber) {
        fail("$fieldName isn't double type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asDouble())
}

fun JsonNode.assertLong(fieldName: String, fieldValue: Long) {
    if (!get(fieldName).isNumber) {
        fail("$fieldName isn't long type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asLong())
}

fun JsonNode.assertInteger(fieldName: String, fieldValue: Int) {
    if (!get(fieldName).isNumber) {
        fail("$fieldName isn't integer type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asInt())
}

fun OpenApiCodec.testDecode(headerType: String, path: String, method: String, code: String?, type: String?, bodyData: String?): Message? {
    val group = MessageGroup.newBuilder()

    val header = message(headerType).apply {
        when(headerType) {
            "Response" -> {
                code?.let {
                    addField(OpenApiCodec.STATUS_CODE_FIELD, code)
                }
                type?.let {
                    addField(OpenApiCodec.HEADERS_FIELD, listOf(message().apply {
                        addField("name", "Content-Type")
                        addField("value", it)
                    }))
                }
            }
            "Request" -> {
                addField(OpenApiCodec.URI_FIELD, path)
                addField(OpenApiCodec.METHOD_FIELD, method)
                type?.let {
                    addField(OpenApiCodec.HEADERS_FIELD, listOf(message().apply {
                        addField("name", "Content-Type")
                        addField("value", it)
                    }))
                }
            }
        }
    }.build()
    group += header

    bodyData?.let {
        group += RawMessage.newBuilder().apply {
            when(headerType) {
                "Response" -> {
                    metadataBuilder.putProperties(OpenApiCodec.URI_PROPERTY, path)
                    metadataBuilder.putProperties(OpenApiCodec.METHOD_PROPERTY, method)
                    code?.let {
                        metadataBuilder.putProperties(OpenApiCodec.CODE_PROPERTY, it)
                    }
                }
                "Request" -> {
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

    if (bodyData != null) {
        Assertions.assertEquals(2, decodeResult.messagesList.size)
        Assertions.assertTrue(decodeResult.messagesList[1].hasMessage())
        return decodeResult.messagesList[1].message
    }
    return null
}

fun OpenApiCodec.testEncode(path: String, method: String, code: String?, type: String?, fillMessage: Message.Builder.() -> Unit): RawMessage {
    val messageType = combineName(path, method, code?:"", type?:"")
    val jsonMessage = message(messageType).apply {
        metadataBuilder.protocol = "openapi"
        fillMessage()
    }.build()

    val messageGroup = MessageGroup.newBuilder()
    messageGroup += jsonMessage

    val resultGroup =  encode(messageGroup.build())


    Assertions.assertEquals(2, resultGroup.messagesList.size)

    Assertions.assertTrue(resultGroup.messagesList[0].hasMessage())
    Assertions.assertTrue(resultGroup.messagesList[1].hasRawMessage())
    val header = resultGroup.messagesList[0].message

    header.apply {
        code?.let {
            assertString(OpenApiCodec.CODE_FIELD, code)
            type?.let {
                this.getList(OpenApiCodec.HEADERS_FIELD)!![0].messageValue.assertString("value", type)
            } ?: Assertions.assertFalse(this.getList(OpenApiCodec.HEADERS_FIELD) == null)
        } ?: run {
            assertString(OpenApiCodec.URI_FIELD, path)
            assertString(OpenApiCodec.METHOD_FIELD, method)
            type?.let {
                this.getList(OpenApiCodec.HEADERS_FIELD)!![0].messageValue.assertString("value", type)
            } ?: Assertions.assertFalse(this.getList(OpenApiCodec.HEADERS_FIELD) == null)
        }
    }
    return  resultGroup.messagesList[1].rawMessage
}

private fun combineName(vararg steps : String): String {
    return buildString {
        for (step in steps) {
            step.split("{", "}", "-", "/", "_").forEach { word ->
                append(word.capitalize())
            }
        }
    }
}