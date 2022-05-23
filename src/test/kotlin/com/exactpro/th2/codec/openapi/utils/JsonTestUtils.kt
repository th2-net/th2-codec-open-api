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

package com.exactpro.th2.codec.openapi.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail

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