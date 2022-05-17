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

import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.common.grpc.Message
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal

sealed class SchemaVisitor<FromType, ToType> {
    val processedFields = mutableListOf<String>()
    abstract val from: FromType
    abstract fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, required: Boolean = false, schemaWriter: SchemaWriter)
    abstract fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean = false)
    abstract fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean = false)
    abstract fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean = false)
    abstract fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean = false)
    abstract fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean = false)
    abstract fun visit(fieldName: String, defaultValue: BigDecimal?, fldStruct: Schema<*>, required: Boolean = false)
    abstract fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean = false)
    abstract fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: ArraySchema, required: Boolean = false)
    abstract fun visitIntegerCollection(fieldName: String, defaultValue: List<Int>?, fldStruct: ArraySchema, required: Boolean = false)
    abstract fun visitBigDecimalCollection(fieldName: String, defaultValue: List<BigDecimal>?, fldStruct: ArraySchema, required: Boolean = false)
    abstract fun visitStringCollection(fieldName: String, defaultValue: List<String>?, fldStruct: ArraySchema, required: Boolean = false)
    abstract fun visitDoubleCollection(fieldName: String, defaultValue: List<Double>?, fldStruct: ArraySchema, required: Boolean = false)
    abstract fun visitFloatCollection(fieldName: String, defaultValue: List<Float>?, fldStruct: ArraySchema, required: Boolean = false)
    abstract fun visitLongCollection(fieldName: String, defaultValue: List<Long>?, fldStruct: ArraySchema, required: Boolean = false)
    abstract fun visitObjectCollection(fieldName: String, defaultValue: List<Any>?, fldStruct: ArraySchema, required: Boolean = false, schemaWriter: SchemaWriter)
    abstract fun getUndefinedFields(fields: MutableSet<String>): Set<String>?
    abstract fun getDiscriminatorValue(fieldName: String): String?

    abstract fun getResult(): ToType

    abstract class EncodeVisitor<T> : SchemaVisitor<T, ByteString>()

    abstract class DecodeVisitor<T> : SchemaVisitor<T, Message.Builder>()
}


