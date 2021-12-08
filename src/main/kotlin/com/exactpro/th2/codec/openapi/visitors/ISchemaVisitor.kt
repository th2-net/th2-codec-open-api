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

package com.exactpro.th2.codec.openapi.visitors

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

interface ISchemaVisitor {
    fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, references: OpenAPI, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitIntegerCollection(fieldName: String, defaultValue: List<Int>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitStringCollection(fieldName: String, defaultValue: List<String>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitDoubleCollection(fieldName: String, defaultValue: List<Double>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitFloatCollection(fieldName: String, defaultValue: List<Float>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitLongCollection(fieldName: String, defaultValue: List<Long>?, fldStruct: Schema<*>, required: Boolean = false)

    fun getResult(): String

}