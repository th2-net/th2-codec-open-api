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
import com.exactpro.th2.common.value.getBigDecimal
import com.exactpro.th2.common.value.getInt
import com.exactpro.th2.common.value.getLong
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode

inline fun <reified T> ArrayNode.putAll(values: List<Value>) = when (T::class) {
    Int::class -> values.forEach { add(it.getInt()) }
    String::class -> values.forEach { add(it.getString()) }
    Float::class, Double::class -> values.forEach { add(it.getBigDecimal()) }
    Boolean::class -> values.forEach { add(it.getString()!!.toBoolean()) }
    Long::class -> values.forEach { add(it.getLong()) }
    else -> error("Unsupported type of ArrayNode: ${T::class.simpleName}")
}

fun JsonNode.getRequiredField(fieldName: String, required: Boolean): JsonNode? = get(fieldName).also { node ->
    if(required && node == null || node is NullNode) {
        error("Field [$fieldName] is required for json [${this.asText()}]")
    }
}

fun JsonNode.getRequiredArray(fieldName: String, required: Boolean) : ArrayNode? = getRequiredField(fieldName, required).apply {
    if(this != null && !this.isArray) {
        error("$fieldName field of json isn't array!")
    }
} as? ArrayNode

fun JsonNode.validateAsBoolean() : Boolean = when {
    isBoolean -> asBoolean()
    else -> error("Cannot convert $this to Boolean")
}

fun JsonNode.validateAsLong() : Long = when {
    isNumber -> asLong()
    else -> error("Cannot convert $this to Long")
}

fun JsonNode.validateAsInteger() : Int = when {
    isNumber -> asInt()
    else -> error("Cannot convert $this to Int")
}

fun JsonNode.validateAsDouble() : Double = when {
    isNumber -> asDouble()
    else -> error("Cannot convert $this to Double")
}

fun JsonNode.validateAsFloat() : Float = when {
    isNumber -> asText().toFloat()
    else -> error("Cannot convert $this to Float")
}

fun JsonNode.validateAsObject() : ObjectNode = when {
    isObject -> this as ObjectNode
    else -> error("Cannot convert $this to Object")
}

