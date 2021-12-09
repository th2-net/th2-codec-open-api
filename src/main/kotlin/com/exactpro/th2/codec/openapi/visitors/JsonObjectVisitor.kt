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

import com.exactpro.th2.codec.openapi.utils.getRequiredField
import com.exactpro.th2.codec.openapi.utils.putAll
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.value.getDouble
import com.exactpro.th2.common.value.getInt
import com.exactpro.th2.common.value.getList
import com.exactpro.th2.common.value.getLong
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema


class JsonObjectVisitor(private val message: Message) : ISchemaVisitor {
    private val rootNode: ObjectNode = mapper.createObjectNode()

    override fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, references: OpenAPI, required: Boolean) {
        TODO("Not yet implemented")
    }

    override fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean) {
        val value = message.getRequiredField(fieldName, required)?.getString()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean) {
        val value = message.getRequiredField(fieldName, required)?.getString()?.toBoolean()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean) {
        val value = message.getRequiredField(fieldName, required)?.getInt()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean) {
        val value = message.getRequiredField(fieldName, required)?.getString()?.toFloat()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean) {
        val value = message.getRequiredField(fieldName, required)?.getDouble()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean) {
        val value = message.getRequiredField(fieldName, required)?.getLong()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: Schema<*>, required: Boolean) {
        message.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Boolean>(values)
        }
    }

    override fun visitIntegerCollection(
        fieldName: String,
        defaultValue: List<Int>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Int>(values)
        }
    }

    override fun visitStringCollection(
        fieldName: String,
        defaultValue: List<String>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<String>(values)
        }
    }

    override fun visitDoubleCollection(
        fieldName: String,
        defaultValue: List<Double>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Double>(values)
        }
    }

    override fun visitFloatCollection(
        fieldName: String,
        defaultValue: List<Float>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Float>(values)
        }
    }

    override fun visitLongCollection(
        fieldName: String,
        defaultValue: List<Long>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Long>(values)
        }
    }

    override fun getResult(): String {
        return rootNode.toPrettyString()
    }

    private fun <T>Schema<*>.checkEnum(value: T?, name: String) {
        if (value != null && enum != null && enum.size > 0 && !enum.contains(value)) {
            error("Enum list of property $name doesn't contain $value")
        }
    }

    companion object {
        private var mapper = ObjectMapper()
    }
}
