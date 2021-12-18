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

package com.exactpro.th2.codec.openapi.writer.visitors.json

import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.ISchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema

class DecodeJsonArrayVisitor(val json: ArrayNode) : ISchemaVisitor<Message> {

    constructor(jsonString: String) : this(mapper.readTree(jsonString) as ArrayNode)

    private val rootMessage = message()

    override fun visit(
        fieldName: String,
        defaultValue: Schema<*>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        throw UnsupportedOperationException("Array visitor supports only collections")
    }

    override fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean) {
        throw UnsupportedOperationException("Array visitor supports only collections")
    }

    override fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean) {
        throw UnsupportedOperationException("Array visitor supports only collections")
    }

    override fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean) {
        throw UnsupportedOperationException("Array visitor supports only collections")
    }

    override fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean) {
        throw UnsupportedOperationException("Array visitor supports only collections")
    }

    override fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean) {
        throw UnsupportedOperationException("Array visitor supports only collections")
    }

    override fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean) {
        throw UnsupportedOperationException("Array visitor supports only collections")
    }

    override fun visitBooleanCollection(
        fieldName: String,
        defaultValue: List<Boolean>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        rootMessage.addField(fieldName, json.map {
            if (it.isBoolean) {
                it.asBoolean()
            } else error("Cannot convert $fieldName=$it to boolean")
        })
    }

    override fun visitIntegerCollection(
        fieldName: String,
        defaultValue: List<Int>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        rootMessage.addField(fieldName, json.map {
            if (it.isInt) {
                it.asInt()
            } else error("Cannot convert $fieldName=$it to integer")
        })
    }

    override fun visitStringCollection(
        fieldName: String,
        defaultValue: List<String>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        rootMessage.addField(fieldName, json.map { it.asText() })
    }

    override fun visitDoubleCollection(
        fieldName: String,
        defaultValue: List<Double>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        rootMessage.addField(fieldName, json.map {
            if (it.isDouble) {
                it.asDouble()
            } else error("Cannot convert $fieldName=$it to double")
        })
    }

    override fun visitFloatCollection(
        fieldName: String,
        defaultValue: List<Float>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        rootMessage.addField(fieldName, json.map {
            if (it.isFloat) {
                it.asText().toFloat()
            } else error("Cannot convert $fieldName=$it to float")
        })
    }

    override fun visitLongCollection(
        fieldName: String,
        defaultValue: List<Long>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        rootMessage.addField(fieldName, json.map {
            if (it.isLong) {
                it.asLong()
            } else error("Cannot convert $fieldName=$it to long")
        })
    }

    override fun visitObjectCollection(
        fieldName: String,
        defaultValue: List<Any>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        rootMessage.addField(fieldName, json.map {
            DecodeJsonObjectVisitor(it).apply {
                SchemaWriter.instance.traverse(this, fldStruct.items)
            }.getResult()
        })
    }

    override fun getResult(): Message = rootMessage.build()

    private companion object {
        val mapper = ObjectMapper()
    }
}