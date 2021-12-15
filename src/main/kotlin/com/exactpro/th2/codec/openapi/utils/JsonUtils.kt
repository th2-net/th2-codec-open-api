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

package com.exactpro.th2.codec.openapi.utils

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.getField
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.value.getDouble
import com.exactpro.th2.common.value.getInt
import com.exactpro.th2.common.value.getLong
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode

inline fun <reified T> ArrayNode.putAll(values: List<Value>) {
    when (T::class) {
        Int::class -> {
            values.forEach {
                add(it.getInt())
            }
        }
        String::class -> {
            values.forEach {
                add(it.getString())
            }
        }
        Double::class -> {
            values.forEach {
                add(it.getDouble())
            }
        }
        Float::class -> {
            values.forEach {
                add(it.getString()?.toFloat())
            }
        }
        Boolean::class -> {
            values.forEach {
                add(it.getString()?.toBoolean())
            }
        }
        Long::class -> {
            values.forEach {
                add(it.getLong())
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

fun JsonNode.getArray(fieldName: String, required: Boolean) : ArrayNode? {
    val field = get(fieldName)
    if (required && field==null) {
        error {"$fieldName array field was required!"}
    }
    if (field!=null && !field.isArray) {
        error {"$fieldName field of json isn't array!"}
    }
    return field as? ArrayNode
}