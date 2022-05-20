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

package com.exactpro.th2.codec.openapi.writer

import com.exactpro.th2.codec.CodecException
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import java.math.BigDecimal


class SchemaWriter constructor(private val openApi: OpenAPI, private val failOnUndefined: Boolean = true) {

    fun traverse(
        schemaVisitor: SchemaVisitor<*, *>,
        msgStructure: Schema<*>
    ) {
        when (val schema = openApi.getEndPoint(msgStructure)) {
            is ComposedSchema -> {
                when {
                    !schema.anyOf.isNullOrEmpty() -> processAnyOf(schema.anyOf, schemaVisitor)
                    !schema.oneOf.isNullOrEmpty() -> processOneOf(schema.oneOf, schemaVisitor)
                    !schema.allOf.isNullOrEmpty() -> processAllOf(schema.allOf, schemaVisitor)
                }
            }
            is ArraySchema -> {
                processArrayProperty(schema, schemaVisitor, ARRAY_TYPE)
            }
            is ObjectSchema -> {
                requireNotNull(schema.properties) {"Properties in object are required: $schema"}
                if (failOnUndefined) {
                    schemaVisitor.getUndefinedFields(schema.properties.keys)?.let {
                        check(it.isEmpty()) { "Undefined fields were found inside of schema: ${it.joinToString()}" }
                    }
                }

                schema.properties.forEach { (name, property) ->
                    processProperty(openApi.getEndPoint(property), schemaVisitor, name, schema.required?.contains(name) ?: false)
                }
            }
        }
    }

    private fun processAllOf(property: List<Schema<*>>, visitor: SchemaVisitor<*, *>) {
        property.forEach {
            traverse(visitor, it)
        }
    }

    private fun processAnyOf(property: List<Schema<*>>, visitor: SchemaVisitor<*, *>) {
        val validSchemes = property.filter(visitor::checkAgainst)
        check(validSchemes.isNotEmpty()) { "Message wasn't valid for any of shames from 'AnyOf' list: ${property.joinToString(", ") { it.`$ref` }}" }
        validSchemes.forEach {
            traverse(visitor, it)
        }
    }

    private fun processOneOf(property: List<Schema<*>>, visitor: SchemaVisitor<*, *>) {
        val validSchemes = property.filter(visitor::checkAgainst)
        check(validSchemes.size == 1) { "Message was valid for [${validSchemes.size}] schemas from OneOf list: ${property.joinToString(", ") { it.`$ref`?: it.type }}" }
        traverse(visitor, validSchemes[0])
    }

    private fun processProperty(property: Schema<*>, visitor: SchemaVisitor<*, *>, name: String, required: Boolean = false) {
        runCatching {
            when(property) {
                is ArraySchema -> processArrayProperty(property, visitor, name, required)
                is StringSchema -> visitor.visit(name, property.default, property, required)
                is IntegerSchema -> when (property.format) {
                    "int64" -> visitor.visit(name, property.default as? Long, property, required)
                    null, "", "int32" -> visitor.visit(name, property.default as? Int, property, required)
                    else -> error("Unsupported format of '${IntegerSchema::class.simpleName}' property $name: ${property.format}")
                }
                is NumberSchema -> when (property.format) {
                    "float" ->visitor.visit(name, property.default?.toFloat(), property, required)
                    "double" -> visitor.visit(name, property.default?.toDouble(), property, required)
                    null, "" -> visitor.visit(name, property.default?.toString(), property, required)
                    else -> visitor.visit(name, property.default, property, required)
                }
                is BooleanSchema -> visitor.visit(name, property.default, property, required)
                is ObjectSchema -> visitor.visit(name, property.default as? Schema<*>, property, required, this)
                else -> error("Unsupported class of property: ${property::class.simpleName}")
            }
        }.onFailure {
            throw CodecException("Cannot parse field [$name]:${property.type}", it)
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun processArrayProperty(property: ArraySchema, visitor: SchemaVisitor<*, *>, name: String, required: Boolean = false) {
        runCatching {
            when(property.items) {
                is IntegerSchema -> when (property.items.format) {
                    "int64" -> visitor.visitLongCollection(name, property.default as? List<Long>, property, required)
                    null, "", "int32" -> visitor.visitIntegerCollection(name, property.default as? List<Int>, property, required)
                    else -> error("Unsupported format of '${IntegerSchema::class.simpleName}' property: ${property.format}")
                }
                is BooleanSchema -> visitor.visitBooleanCollection(name, property.default as? List<Boolean>, property, required)
                is NumberSchema-> when (property.items.format) {
                    "float" -> visitor.visitFloatCollection(name, property.default as? List<Float>, property, required)
                    "double" -> visitor.visitDoubleCollection(name, property.default as? List<Double>, property, required)
                    null, "" -> visitor.visitStringCollection(name, property.default as? List<String>, property, required)
                    else -> visitor.visitBigDecimalCollection(name, property.default as? List<BigDecimal>, property, required)
                }
                is StringSchema -> visitor.visitStringCollection(name, property.default as? List<String>, property, required)
                is ObjectSchema -> visitor.visitObjectCollection(name, property.default as? List<Any>, property, required, this)
                else -> error("Unsupported type of property")
            }
        }.onFailure {
            throw CodecException("Cannot parse array field [$name] inside of schema with type ${property.type}", it)
        }
    }

    companion object {
        const val ARRAY_TYPE = "array"
    }
}