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

import com.exactpro.th2.codec.openapi.utils.getField
import com.exactpro.th2.codec.openapi.utils.putAll
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor.EncodeVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.toJson
import com.exactpro.th2.common.value.getList
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal

class EncodeJsonArrayVisitor(override val from: Message) : EncodeVisitor<Message>() {

    private val rootNode: ArrayNode = mapper.createArrayNode()

    companion object {
        private var mapper = ObjectMapper()
    }

    override fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, required: Boolean, schemaWriter: SchemaWriter) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun visit(fieldName: String, defaultValue: BigDecimal?, fldStruct: Schema<*>, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")

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
        }?.forEach(rootNode::add)
    }

    override fun getUndefinedFields(fields: MutableSet<String>): Nothing? = null

    override fun getResult(): ByteString {
        return if (rootNode.isEmpty) {
            ByteString.EMPTY
        } else {
            ByteString.copyFrom(rootNode.toString().toByteArray())
        }
    }

    private inline fun <reified T> ArrayNode.putListFrom(message: Message, name: String, defaultValue: List<T>?, required: Boolean) {
        message.getField(name, required)?.getList()?.let { this.putAll<T>(it) } ?: defaultValue?.let(::putAll)
    }

    override fun getDiscriminatorValue(fieldName: String): String? {
        return null
    }
}