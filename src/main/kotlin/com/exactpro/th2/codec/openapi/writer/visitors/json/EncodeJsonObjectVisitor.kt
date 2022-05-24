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
import com.exactpro.th2.codec.openapi.utils.getBoolean
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.getField
import com.exactpro.th2.codec.openapi.utils.putAll
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.value.getBigDecimal
import com.exactpro.th2.common.value.getList
import com.exactpro.th2.common.value.getLong
import com.exactpro.th2.common.value.getMessage
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ByteArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.EmailSchema
import io.swagger.v3.oas.models.media.FileSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.PasswordSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.UUIDSchema
import java.math.BigDecimal

open class EncodeJsonObjectVisitor(override val from: Message, override val openAPI: OpenAPI) : SchemaVisitor.EncodeVisitor<Message>() {
    internal val rootNode: ObjectNode = mapper.createObjectNode()

    override fun visit(fieldName: String, fldStruct: Schema<*>, required: Boolean, throwUndefined: Boolean) {
        from.getField(fieldName, required)?.let { field ->
            when {
                field.kindCase.number == 1 -> return
                !field.hasMessageValue() -> error("$fieldName is not an message: ${field.kindCase}")
            }
            val message = field.getMessage()!!
            val visitor = EncodeJsonObjectVisitor(message, openAPI)
            val writer = SchemaWriter(openAPI)
            writer.traverse(visitor, fldStruct, throwUndefined)
            rootNode.set<ObjectNode>(fieldName, visitor.rootNode)
        } ?: fldStruct.default?.let { error("Default values isn't supported for objects") }
    }

    override fun visit(fieldName: String, fldStruct: ArraySchema, required: Boolean, throwUndefined: Boolean) {
        val itemSchema = openAPI.getEndPoint(fldStruct.items)

        from.getField(fieldName, required)?.let { field ->
            when {
                field.kindCase.number == 1 -> return
                !field.hasListValue() -> error("$fieldName is not an list: ${field.kindCase}")
            }
            val listOfValues = field.getList()!!

            when (itemSchema) {
                is NumberSchema -> rootNode.putArray(fieldName).putAll<BigDecimal>(listOfValues)
                is IntegerSchema -> rootNode.putArray(fieldName).putAll<Long>(listOfValues)
                is BooleanSchema -> rootNode.putArray(fieldName).putAll<Boolean>(listOfValues)
                is StringSchema -> rootNode.putArray(fieldName).putAll<String>(listOfValues)
                is BinarySchema, is ByteArraySchema, is DateSchema, is DateTimeSchema, is EmailSchema, is FileSchema, is MapSchema, is PasswordSchema, is UUIDSchema -> throw UnsupportedOperationException("${itemSchema::class.simpleName} for json array isn't supported for now")
                else -> rootNode.putArray(fieldName).apply {
                    val listOfNodes = mutableListOf<ObjectNode>()
                    val writer = SchemaWriter(openAPI)
                    listOfValues.forEach {
                        EncodeJsonObjectVisitor(checkNotNull(it.getMessage()) { " Value from list [$fieldName] must be message" }, openAPI).let { visitor ->
                            writer.traverse(visitor, itemSchema, throwUndefined)
                            visitor.rootNode.run(listOfNodes::add)
                        }
                    }
                    listOfNodes.forEach { add(it) }
                }
            }
        } ?: fldStruct.default?.let { error("Default values isn't supported for arrays") }
    }

    override fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean) {
        from.getField(fieldName, required)?.let { field ->
            when {
                field.kindCase.number == 1 -> return
                !field.hasMessageValue() -> error("$fieldName is not an message: ${field.kindCase}")
            }
            val message = field.getMessage()!!

            val visitor = EncodeJsonObjectVisitor(message, openAPI)
            val writer = SchemaWriter(openAPI)
            writer.traverse(visitor, fldStruct, false)
            rootNode.set<ObjectNode>(fieldName, visitor.rootNode)
        } ?: fldStruct.default?.let { error("Default values isn't supported for objects") }
    }

    override fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required), fldStruct, Value::getBigDecimal) {
        rootNode.put(fieldName, it)
    }

    override fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required), fldStruct, Value::getLong) {
        rootNode.put(fieldName, it.toLong())
    }

    override fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required), fldStruct, Value::getString) {
        rootNode.put(fieldName, it)
    }

    override fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required), fldStruct, Value::getBoolean) {
        rootNode.put(fieldName, it)
    }

    override fun getFieldNames(): Collection<String> = from.fieldsMap.keys
    override fun getResult(): ByteString = ByteString.copyFrom(rootNode.toString().toByteArray())


    private inline fun <reified T> visitPrimitive(fieldName: String, fieldValue: Value?, fldStruct: Schema<T>, convert: (Value) -> T, put: (T) -> Unit) {
        fieldValue?.let { value ->
            val converted = checkNotNull(convert(value)) { "Cannot convert field $fieldName to ${T::class.simpleName}" }
            fldStruct.checkEnum(converted, fieldName)
            put(converted)
        } ?: fldStruct.default?.run(put)
    }


    private companion object {
        val mapper = ObjectMapper().apply {
            nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
        }
    }

}