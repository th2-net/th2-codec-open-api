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

package com.exactpro.th2.codec.openapi.writer.visitors

import com.exactpro.th2.codec.openapi.utils.getExclusiveProperties
import com.exactpro.th2.common.grpc.Message
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

sealed class SchemaVisitor<FromType, ToType> {
    protected abstract val settings: VisitorSettings
    abstract val from: FromType
    abstract fun getResult(): ToType
    protected abstract fun getFieldNames(): Collection<String>
    abstract fun visit(fieldName: String, fldStruct: Schema<*>, required: Boolean, throwUndefined: Boolean = true)
    abstract fun visit(fieldName: String, fldStruct: ArraySchema, required: Boolean, throwUndefined: Boolean = true)
    abstract fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: DateSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: DateTimeSchema, required: Boolean)

    fun oneOf(list: List<Schema<*>>): Schema<*> = chooseOneOf(list.filter(this::checkAgainst))

    fun anyOf(list: List<Schema<*>>): List<Schema<*>> = list.filter(this::checkAgainst).also {
        if (it.isEmpty()) {
            throw IllegalStateException("AnyOf statement had zero valid schemas")
        }
    }

    fun allOf(list: List<Schema<*>>): List<Schema<*>> = list.filter(this::checkAgainst).also {
        if (list.size != it.size) {
            throw IllegalStateException("AllOf statement have only ${it.size} valid schemas of ${list.size} available")
        }
    }

    fun checkUndefined(objectSchema: Schema<*>) {
        val names = objectSchema.properties.keys
        val undefined = getFieldNames().filter { !names.contains(it) }
        if (undefined.isNotEmpty()) {
            throw IllegalStateException("Message have undefined fields: ${undefined.joinToString(", ")}")
        }
    }

    private fun checkAgainst(fldStruct: Schema<*>): Boolean = fldStruct.required.isNullOrEmpty() || getFieldNames().containsAll(fldStruct.required)

    private fun chooseOneOf(list: List<Schema<*>>): Schema<*> = when(list.size) {
        0 -> throw IllegalStateException("OneOf statement have 0 valid schemas")
        1 -> list[0]
        else -> {
            val objectFieldNames = getFieldNames()
            list.find { schema ->
                val exclusiveNames = schema.getExclusiveProperties(list.toMutableList().apply { remove(schema) })
                objectFieldNames.find { exclusiveNames.contains(it) } != null
            } ?: list[0]
        }
    }

    abstract class EncodeVisitor<T> : SchemaVisitor<T, ByteString>()
    abstract class DecodeVisitor<T> : SchemaVisitor<T, Message.Builder>()
}

data class VisitorSettings(val openAPI: OpenAPI, val dateFormat: SimpleDateFormat, val dateTimeFormat: DateTimeFormatter)

