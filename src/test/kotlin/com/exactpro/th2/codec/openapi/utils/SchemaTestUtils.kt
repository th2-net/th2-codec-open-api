package com.exactpro.th2.codec.openapi/*
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

import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import java.math.BigDecimal

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
            return IntegerSchema().apply {
                type = "int32"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Int).toBigDecimal())
                }
            }
        }
        Long::class -> {
            return IntegerSchema().apply {
                type = "int64"
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
        BigDecimal::class -> {
            return NumberSchema().apply {
                type = "number"
                format = "-"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Double).toBigDecimal())
                }
            }
        }
        else -> {
            throw UnsupportedOperationException("CreateTestSchema method in test utils don't supports ${T::class} type of value")
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