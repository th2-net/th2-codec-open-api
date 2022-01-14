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
import com.exactpro.th2.codec.openapi.utils.getRequiredField
import com.exactpro.th2.codec.openapi.utils.putAll
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor.EncodeVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.toJson
import com.exactpro.th2.common.value.getDouble
import com.exactpro.th2.common.value.getInt
import com.exactpro.th2.common.value.getList
import com.exactpro.th2.common.value.getLong
import com.exactpro.th2.common.value.getMessage
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema


class EncodeJsonObjectVisitor(override val from: Message) : EncodeVisitor<Message>() {
    private val rootNode: ObjectNode = mapper.createObjectNode()

    override fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, required: Boolean) {
        from.getRequiredField(fieldName, required)?.getMessage()?.let { nextMessage ->
            val visitor = EncodeJsonObjectVisitor(nextMessage)
            SchemaWriter.instance.traverse(visitor, fldStruct)
            rootNode.set<ObjectNode>(fieldName, visitor.rootNode)
        }
    }

    override fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean) {
        val value = from.getRequiredField(fieldName, required)?.getString()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean) {
        val value = from.getRequiredField(fieldName, required)?.getString()?.toBoolean()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean) {
        val value = from.getRequiredField(fieldName, required)?.getInt()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean) {
        val value = from.getRequiredField(fieldName, required)?.getString()?.toFloat()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean) {
        val value = from.getRequiredField(fieldName, required)?.getDouble()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean) {
        val value = from.getRequiredField(fieldName, required)?.getLong()
        fldStruct.checkEnum(value, fieldName)
        rootNode.put(fieldName, value ?: defaultValue)
    }

    override fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: ArraySchema, required: Boolean) {
        from.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Boolean>(values)
        }
    }

    override fun visitIntegerCollection(fieldName: String, defaultValue: List<Int>?, fldStruct: ArraySchema, required: Boolean) {
        from.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Int>(values)
        }
    }

    override fun visitStringCollection(fieldName: String, defaultValue: List<String>?, fldStruct: ArraySchema, required: Boolean) {
        from.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<String>(values)
        }
    }

    override fun visitDoubleCollection(fieldName: String, defaultValue: List<Double>?, fldStruct: ArraySchema, required: Boolean) {
        from.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Double>(values)
        }
    }

    override fun visitFloatCollection(fieldName: String, defaultValue: List<Float>?, fldStruct: ArraySchema, required: Boolean) {
        from.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Float>(values)
        }
    }

    override fun visitLongCollection(fieldName: String, defaultValue: List<Long>?, fldStruct: ArraySchema, required: Boolean) {
        from.getRequiredField(fieldName, required)?.getList()?.let { values ->
            rootNode.putArray(fieldName).putAll<Long>(values)
        }
    }

    override fun visitObjectCollection(fieldName: String, defaultValue: List<Any>?, fldStruct: ArraySchema, required: Boolean) {
        from.getRequiredField(fieldName, required)?.getList()?.map {
            if (!it.hasMessageValue()) error("Cannot convert $fieldName=${it.toJson(true)} to json object")
            EncodeJsonObjectVisitor(it.messageValue).apply {
                SchemaWriter.instance.traverse(this, fldStruct.items)
            }.getNode()
        }?.run(rootNode.putArray(fieldName)::addAll)
    }

    override fun visitUndefinedFields(fields: MutableSet<String>): Set<String> = this.from.fieldsMap.keys.apply { removeAll(fields) }

    override fun getResult(): ByteString = ByteString.copyFrom(rootNode.toString().toByteArray())

    fun getNode() = rootNode

    private companion object {
        val mapper = ObjectMapper()
    }
}
