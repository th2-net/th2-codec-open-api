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

import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

inline fun <reified T> ArrayNode.putAll(values: List<Value>) {
    when (T::class) {
        Int::class -> {
            values.forEach {
                add(it.getString()!!.toInt())
            }
        }
        String::class -> {
            values.forEach {
                add(it.getString())
            }
        }
        Double::class -> {
            values.forEach {
                add(it.getString()!!.toDouble())
            }
        }
        Float::class -> {
            values.forEach {
                add(it.getString()!!.toFloat())
            }
        }
        Boolean::class -> {
            values.forEach {
                add(it.getString()!!.toBoolean())
            }
        }
        Long::class -> {
            values.forEach {
                add(it.getString()!!.toLong())
            }
        }
        else -> {
            error("Unsupported type of ArrayNode: ${T::class.simpleName}")
        }
    }
}

fun JsonNode.getRequiredField(fieldName: String, required: Boolean): JsonNode? {
    val result = get(fieldName)
    return if (required) {
        checkNotNull(result) {"Field [$fieldName] is required for json [${this.asText()}]"}
    } else result
}

fun JsonNode.getRequiredArray(fieldName: String, required: Boolean) : ArrayNode? {
    val field = get(fieldName)
    if (required && field==null) {
        error {"$fieldName array field was required!"}
    }
    if (field!=null && !field.isArray) {
        error {"$fieldName field of json isn't array!"}
    }
    return field as? ArrayNode
}

fun JsonNode.validateAsBoolean() : Boolean {
    return if (isBoolean) {
        asText().toBoolean()
    } else error("Cannot convert $this to Boolean")
}


fun JsonNode.validateAsLong() : Long {
    return if (isNumber) {
        asLong()
    } else error("Cannot convert $this to Long")
}

fun JsonNode.validateAsInteger() : Int {
    return if (isNumber) {
        asInt()
    } else error("Cannot convert $this to Int")
}

fun JsonNode.validateAsDouble() : Double {
    return if (isNumber) {
        asDouble()
    } else error("Cannot convert $this to Double")
}

fun JsonNode.validateAsFloat() : Float {
    return if (isNumber) {
        asText().toFloat()
    } else error("Cannot convert $this to Float")
}

fun JsonNode.validateAsObject() : ObjectNode {
    return if (isObject) {
        this as ObjectNode
    } else error("Cannot convert $this to Object")
}

