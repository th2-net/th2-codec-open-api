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
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.getField
import com.exactpro.th2.codec.openapi.utils.getRequiredArray
import com.exactpro.th2.codec.openapi.utils.validateAsBigDecimal
import com.exactpro.th2.codec.openapi.utils.validateAsBoolean
import com.exactpro.th2.codec.openapi.utils.validateAsLong
import com.exactpro.th2.codec.openapi.utils.validateAsObject
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
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
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.PasswordSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.UUIDSchema
import java.lang.IllegalStateException

open class DecodeJsonObjectVisitor(override val from: JsonNode, override val openAPI: OpenAPI) : SchemaVisitor.DecodeVisitor<JsonNode>() {

    constructor(jsonString: String, openAPI: OpenAPI) : this(mapper.readTree(jsonString), openAPI)

    internal val rootMessage = message()
    private val fromObject = from as ObjectNode

    override fun visit(fieldName: String, fldStruct: Schema<*>, required: Boolean, throwUndefined: Boolean) {
        fromObject.getField(fieldName, required)?.let { message ->
            val visitor = DecodeJsonObjectVisitor(message.validateAsObject(), openAPI)
            val writer = SchemaWriter(openAPI)
            writer.traverse(visitor, fldStruct, throwUndefined)
            rootMessage.addField(fieldName, visitor.rootMessage)
        } ?: fldStruct.default?.let { error("Default values isn't supported for objects") }
    }

    override fun visit(fieldName: String, fldStruct: ArraySchema, required: Boolean, throwUndefined: Boolean) {
        val itemSchema = openAPI.getEndPoint(fldStruct.items)

        fromObject.getRequiredArray(fieldName, required)?.let { arrayNode ->
            when (itemSchema) {
                is NumberSchema -> rootMessage.addField(fieldName, arrayNode.map { it.validateAsBigDecimal() })
                is IntegerSchema -> rootMessage.addField(fieldName, arrayNode.map { it.validateAsLong() })
                is BooleanSchema -> rootMessage.addField(fieldName, arrayNode.map { it.validateAsBoolean() })
                is StringSchema -> rootMessage.addField(fieldName, arrayNode.map { it.asText() })
                is BinarySchema, is ByteArraySchema, is DateSchema, is DateTimeSchema, is EmailSchema, is FileSchema, is MapSchema, is PasswordSchema, is UUIDSchema -> throw UnsupportedOperationException("${itemSchema::class.simpleName} for json array isn't supported for now")
                else -> rootMessage.addField(fieldName, mutableListOf<Message>().apply {
                    arrayNode.forEach {
                        DecodeJsonObjectVisitor(checkNotNull(it.validateAsObject()) { " Value from list [$fieldName] must be message" }, openAPI).let { visitor ->
                            SchemaWriter(openAPI).traverse(visitor, itemSchema, throwUndefined)
                            visitor.rootMessage.build().run(this::add)
                        }
                    }
                })
            }
        } ?: fldStruct.default?.let { error("Default values isn't supported for arrays") }
    }

    override fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean) {
        fromObject.getField(fieldName, required)?.let { message ->
            val visitor = DecodeJsonObjectVisitor(message.validateAsObject(), openAPI)
            val writer = SchemaWriter(openAPI)
            writer.traverse(visitor, fldStruct, false)
            rootMessage.addField(fieldName, visitor.rootMessage)
        } ?: fldStruct.default?.let { error("Default values isn't supported for objects") }
    }

    override fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean) = visitPrimitive(fieldName, fromObject.getField(fieldName, required)?.validateAsBigDecimal(), fldStruct)
    override fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean) = visitPrimitive(fieldName, fromObject.getField(fieldName, required)?.validateAsLong(), fldStruct)
    override fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean) = visitPrimitive(fieldName, fromObject.getField(fieldName, required)?.asText(), fldStruct)
    override fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean) = visitPrimitive(fieldName, fromObject.getField(fieldName, required)?.validateAsBoolean(), fldStruct)

    private fun <T> visitPrimitive(fieldName: String, value: T?, fldStruct: Schema<T>) {
        (value?.also { fldStruct.checkEnum(it, fieldName) } ?: fldStruct.default)?.let {
            rootMessage.addFields(fieldName, it)
        }
    }

    override fun getResult(): Message.Builder = rootMessage

    private companion object {
        val mapper = ObjectMapper().apply {
            nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
        }
    }

    override fun checkUndefined(objectSchema: Schema<*>) {
        val names = objectSchema.properties.keys
        val undefined = fromObject.fieldNames().asSequence().toList().filter { !names.contains(it) }
        if (undefined.isNotEmpty()) {
            throw IllegalStateException("Message have undefined fields: ${undefined.joinToString(", ")}")
        }
    }

    override fun checkAgainst(fldStruct: ObjectSchema): Boolean = fldStruct.required.isNullOrEmpty() || fromObject.fieldNames().asSequence().toList().containsAll(fldStruct.required)
}