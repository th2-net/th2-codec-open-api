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

import com.exactpro.th2.codec.openapi.utils.validateAsBigDecimal
import com.exactpro.th2.codec.openapi.utils.validateAsBoolean
import com.exactpro.th2.codec.openapi.utils.validateAsDouble
import com.exactpro.th2.codec.openapi.utils.validateAsFloat
import com.exactpro.th2.codec.openapi.utils.validateAsInteger
import com.exactpro.th2.codec.openapi.utils.validateAsLong
import com.exactpro.th2.codec.openapi.utils.validateAsObject
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor.DecodeVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal

class DecodeJsonArrayVisitor(override val from: ArrayNode) : DecodeVisitor<ArrayNode>() {

    constructor(jsonString: String) : this(mapper.readTree(jsonString) as ArrayNode)

    private val rootMessage = message()

    override fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, required: Boolean, schemaWriter: SchemaWriter) {
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

    override fun visit(fieldName: String, defaultValue: BigDecimal?, fldStruct: Schema<*>, required: Boolean) {
        throw UnsupportedOperationException("Array visitor supports only collections")
    }

    override fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: ArraySchema, required: Boolean)  = rootMessage.putListFrom(from, fieldName, defaultValue, required, JsonNode::validateAsBoolean)

    override fun visitIntegerCollection(fieldName: String, defaultValue: List<Int>?, fldStruct: ArraySchema, required: Boolean) = rootMessage.putListFrom(from, fieldName, defaultValue, required, JsonNode::validateAsInteger)

    override fun visitStringCollection(fieldName: String, defaultValue: List<String>?, fldStruct: ArraySchema, required: Boolean) = rootMessage.putListFrom(from, fieldName, defaultValue, required, JsonNode::asText)

    override fun visitDoubleCollection(fieldName: String, defaultValue: List<Double>?, fldStruct: ArraySchema, required: Boolean) = rootMessage.putListFrom(from, fieldName, defaultValue, required, JsonNode::validateAsDouble)

    override fun visitFloatCollection(fieldName: String, defaultValue: List<Float>?, fldStruct: ArraySchema, required: Boolean) = rootMessage.putListFrom(from, fieldName, defaultValue, required, JsonNode::validateAsFloat)

    override fun visitLongCollection(fieldName: String, defaultValue: List<Long>?, fldStruct: ArraySchema, required: Boolean) = rootMessage.putListFrom(from, fieldName, defaultValue, required, JsonNode::validateAsLong)

    override fun visitBigDecimalCollection(fieldName: String, defaultValue: List<BigDecimal>?, fldStruct: ArraySchema, required: Boolean) = rootMessage.putListFrom(from, fieldName, defaultValue, required) { this.validateAsBigDecimal().toPlainString() }

    override fun visitObjectCollection(fieldName: String, defaultValue: List<Any>?, fldStruct: ArraySchema, required: Boolean, schemaWriter: SchemaWriter) {
        rootMessage.addField(fieldName, from.map {
            DecodeJsonObjectVisitor(it.validateAsObject()).apply {
                schemaWriter.traverse(this, fldStruct.items)
            }.getResult()
        })
    }

    override fun getUndefinedFields(fields: MutableSet<String>): Nothing? = null

    override fun getResult(): Message.Builder = rootMessage

    private inline fun <reified T> Message.Builder.putListFrom(node: ArrayNode, name: String, defaultValue: List<T>?, required: Boolean, extract: JsonNode.() -> T) {
        if (node.isEmpty) {
            when {
                required -> error("$name field is required but array node was empty")
                !defaultValue.isNullOrEmpty() -> this.addField(name, defaultValue)
            }
        } else {
            this.addField(name, node.map(extract))
        }
    }

    private companion object {
        val mapper = ObjectMapper()
    }

}