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
import com.exactpro.th2.codec.openapi.utils.getField
import com.exactpro.th2.codec.openapi.utils.putAll
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor.EncodeVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.toJson
import com.exactpro.th2.common.value.getBigDecimal
import com.exactpro.th2.common.value.getDouble
import com.exactpro.th2.common.value.getInt
import com.exactpro.th2.common.value.getList
import com.exactpro.th2.common.value.getLong
import com.exactpro.th2.common.value.getMessage
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal


class EncodeJsonObjectVisitor(override val from: Message) : EncodeVisitor<Message>() {

    private val rootNode: ObjectNode = mapper.createObjectNode()

    override fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, required: Boolean, schemaWriter: SchemaWriter) {
        from.getField(fieldName, required)?.getMessage()?.let { nextMessage ->
            val visitor = EncodeJsonObjectVisitor(nextMessage)
            schemaWriter.traverse(visitor, fldStruct)
            rootNode.set<ObjectNode>(fieldName, visitor.rootNode)
        }
    }

    override fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean) {
        from.getField(fieldName, required)?.getString()?.let { value ->
            fldStruct.checkEnum(value, fieldName)
            rootNode.put(fieldName, value)
        } ?: defaultValue?.let {
            rootNode.put(fieldName, defaultValue)
        }
    }

    override fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean) {
        from.getField(fieldName, required)?.getString()?.toBoolean()?.let { value ->
            fldStruct.checkEnum(value, fieldName)
            rootNode.put(fieldName, value)
        } ?: defaultValue?.let {
            rootNode.put(fieldName, defaultValue)
        }
    }

    override fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean) {
        from.getField(fieldName, required)?.getInt()?.let { value ->
            fldStruct.checkEnum(value, fieldName)
            rootNode.put(fieldName, value)
        } ?: defaultValue?.let {
            rootNode.put(fieldName, defaultValue)
        }
    }

    override fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean) {
        from.getField(fieldName, required)?.getString()?.toFloat()?.let { value ->
            fldStruct.checkEnum(value, fieldName)
            rootNode.put(fieldName, value)
        } ?: defaultValue?.let {
            rootNode.put(fieldName, defaultValue)
        }
    }

    override fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean) {
        from.getField(fieldName, required)?.getDouble()?.let { value ->
            fldStruct.checkEnum(value, fieldName)
            rootNode.put(fieldName, value)
        } ?: defaultValue?.let {
            rootNode.put(fieldName, defaultValue)
        }
    }

    override fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean) {
        from.getField(fieldName, required)?.getLong()?.let { value ->
            fldStruct.checkEnum(value, fieldName)
            rootNode.put(fieldName, value)
        } ?: defaultValue?.let {
            rootNode.put(fieldName, defaultValue)
        }
    }

    override fun visit(fieldName: String, defaultValue: BigDecimal?, fldStruct: Schema<*>, required: Boolean) {
        from.getField(fieldName, required)?.getBigDecimal()?.let { value ->
            fldStruct.checkEnum(value, fieldName)
            rootNode.put(fieldName, value)
        } ?: defaultValue?.let {
            rootNode.put(fieldName, defaultValue)
        }
    }

    override fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: ArraySchema, required: Boolean) = rootNode.putListFrom(from, fieldName, defaultValue, required)

    override fun visitIntegerCollection(fieldName: String, defaultValue: List<Int>?, fldStruct: ArraySchema, required: Boolean) = rootNode.putListFrom(from, fieldName, defaultValue, required)

    override fun visitStringCollection(fieldName: String, defaultValue: List<String>?, fldStruct: ArraySchema, required: Boolean) = rootNode.putListFrom(from, fieldName, defaultValue, required)

    override fun visitDoubleCollection(fieldName: String, defaultValue: List<Double>?, fldStruct: ArraySchema, required: Boolean) = rootNode.putListFrom(from, fieldName, defaultValue, required)

    override fun visitFloatCollection(fieldName: String, defaultValue: List<Float>?, fldStruct: ArraySchema, required: Boolean) = rootNode.putListFrom(from, fieldName, defaultValue, required)

    override fun visitLongCollection(fieldName: String, defaultValue: List<Long>?, fldStruct: ArraySchema, required: Boolean) = rootNode.putListFrom(from, fieldName, defaultValue, required)

    override fun visitBigDecimalCollection(fieldName: String, defaultValue: List<BigDecimal>?, fldStruct: ArraySchema, required: Boolean) = rootNode.putListFrom(from, fieldName, defaultValue, required)

    override fun visitObjectCollection(fieldName: String, defaultValue: List<Any>?, fldStruct: ArraySchema, required: Boolean, schemaWriter: SchemaWriter) {
        from.getField(fieldName, required)?.getList()?.map {
            if (!it.hasMessageValue()) error("Cannot convert $fieldName=${it.toJson(true)} to json object")
            EncodeJsonObjectVisitor(it.messageValue).apply {
                schemaWriter.traverse(this, fldStruct.items)
            }.getNode()
        }?.run(rootNode.putArray(fieldName)::addAll)
    }

    override fun getUndefinedFields(fields: MutableSet<String>): Set<String> = this.from.fieldsMap.keys - fields

    override fun getResult(): ByteString = ByteString.copyFrom(rootNode.toString().toByteArray())

    fun getNode() = rootNode

    private inline fun <reified T> ObjectNode.putListFrom(message: Message, name: String, defaultValue: List<T>?, required: Boolean) {
        message.getField(name, required)?.getList()?.let { values ->
            this.putArray(name).putAll<T>(values)
        } ?: defaultValue?.let { list ->
            this.putArray(name).putAll(list)
        }
    }

    override fun checkAgainst(message: Schema<*>): Boolean {
        val fieldNames = from.fieldsMap.keys
        return message.required.filterNot { it in fieldNames }.isEmpty()
    }

    private companion object {
        val mapper = ObjectMapper().apply {
            nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
        }
    }

}
