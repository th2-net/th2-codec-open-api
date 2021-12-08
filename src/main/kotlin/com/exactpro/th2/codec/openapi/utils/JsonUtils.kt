package com.exactpro.th2.codec.openapi.utils

import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.value.getDouble
import com.exactpro.th2.common.value.getInt
import com.exactpro.th2.common.value.getLong
import com.exactpro.th2.common.value.getString
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