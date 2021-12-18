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

import com.exactpro.th2.codec.openapi.utils.checkEnum
import com.exactpro.th2.codec.openapi.utils.getArray
import com.exactpro.th2.codec.openapi.utils.getRequiredField
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.ISchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema

class DecodeJsonObjectVisitor(val json: JsonNode) : ISchemaVisitor<Message> {
    private val rootMessage = message()

    constructor(jsonString: String) : this(mapper.readTree(jsonString))


    override fun visit(
        fieldName: String,
        defaultValue: Schema<*>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        json.getRequiredField(fieldName, required)?.let {
            val visitor = DecodeJsonObjectVisitor(it)
            SchemaWriter.instance.traverse(visitor, fldStruct)
            rootMessage.addFields(fieldName, visitor.rootMessage.build())
        }
    }

    override fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asText()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asBoolean()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asInt()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asText()?.toFloat()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asDouble()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asLong()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visitBooleanCollection(
        fieldName: String,
        defaultValue: List<Boolean>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.let { array ->
            rootMessage.addField(fieldName, array.map {
                if (it.isBoolean) {
                    it.asBoolean()
                } else error("Cannot convert $fieldName=$it to boolean")
            })
        }
    }

    override fun visitIntegerCollection(
        fieldName: String,
        defaultValue: List<Int>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.let { array ->
            rootMessage.addField(fieldName, array.map {
                if (it.isNumber) {
                    it.asInt()
                } else error("Cannot convert $fieldName=$it to integer")
            })
        }
    }

    override fun visitStringCollection(
        fieldName: String,
        defaultValue: List<String>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.map { it.asText() }?.let {
            rootMessage.addField(fieldName, it)
        }
    }

    override fun visitDoubleCollection(
        fieldName: String,
        defaultValue: List<Double>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.let { array ->
            rootMessage.addField(fieldName, array.map {
                if (it.isNumber) {
                    it.asDouble()
                } else error("Cannot convert $fieldName=$it to double")
            })
        }
    }

    override fun visitFloatCollection(
        fieldName: String,
        defaultValue: List<Float>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.let { array ->
            rootMessage.addField(fieldName, array.map {
                if (it.isNumber) {
                    it.asText().toFloat()
                } else error("Cannot convert $fieldName=$it to float")
            })
        }
    }

    override fun visitLongCollection(
        fieldName: String,
        defaultValue: List<Long>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.let { array ->
            rootMessage.addField(fieldName, array.map {
                if (it.isNumber) {
                    it.asLong()
                } else error("Cannot convert $fieldName=$it to long")
            })
        }
    }

    override fun visitObjectCollection(
        fieldName: String,
        defaultValue: List<Any>?,
        fldStruct: ArraySchema,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.let { array ->
            rootMessage.addField(fieldName, array.map {
                DecodeJsonObjectVisitor(it).apply {
                    SchemaWriter.instance.traverse(this, fldStruct.items)
                }.getResult()
            })
        }
    }

    override fun getResult(): Message = rootMessage.build()

    private companion object {
        val mapper = ObjectMapper()
    }
}