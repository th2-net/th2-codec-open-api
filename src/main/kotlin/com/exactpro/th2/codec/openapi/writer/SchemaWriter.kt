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
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.util.SchemaTypeUtil.BOOLEAN_TYPE
import io.swagger.v3.parser.util.SchemaTypeUtil.INTEGER_TYPE
import io.swagger.v3.parser.util.SchemaTypeUtil.NUMBER_TYPE
import io.swagger.v3.parser.util.SchemaTypeUtil.OBJECT_TYPE
import io.swagger.v3.parser.util.SchemaTypeUtil.STRING_TYPE
import java.math.BigDecimal


class SchemaWriter constructor(private val openApi: OpenAPI, private val failOnUndefined: Boolean = true) {

    fun traverse(
        schemaVisitor: SchemaVisitor<*, *>,
        msgStructure: Schema<*>
    ) {
        val schema = openApi.getEndPoint(msgStructure)

        when (schema.type) {
            ARRAY_TYPE -> processProperty(schema, schemaVisitor, ARRAY_TYPE)
            OBJECT_TYPE -> {
                requireNotNull(schema.properties) {"Properties in object are required: $schema"}
                if (failOnUndefined) {
                    schemaVisitor.getUndefinedFields(schema.properties.keys)?.let {
                        check(it.isEmpty()) { "Undefined fields were found inside of ${schema.name}: ${it.joinToString()}" }
                    }
                }

                schema.properties.forEach { (name, property) ->
                    processProperty(openApi.getEndPoint(property), schemaVisitor, name, schema.required?.contains(name) ?: false)
                }
            }
        }
    }

    private fun processProperty(property: Schema<*>, visitor: SchemaVisitor<*, *>, name: String, required: Boolean = false) {
        runCatching {
            when(property.type) {
                ARRAY_TYPE -> processArrayProperty(property as ArraySchema, visitor, name, required)
                INTEGER_TYPE -> when (property.format) {
                    "int64" -> visitor.visit(name, property.default as? Long, property, required)
                    null, "", "int32" -> visitor.visit(name, property.default as? Int, property, required)
                    else -> error("Unsupported format of '$INTEGER_TYPE' property $name: ${property.format}")
                }
                BOOLEAN_TYPE -> visitor.visit(name, property.default as? Boolean, property, required)
                NUMBER_TYPE -> when (property.format) {
                    "float" -> visitor.visit(name, property.default as? Float, property, required)
                    "double" -> visitor.visit(name, property.default as? Double, property, required)
                    "-" -> visitor.visit(name, property.default as? BigDecimal, property, required)
                    null, "" -> visitor.visit(name, property.default as? String, property, required)
                    else -> error("Unsupported format of '$NUMBER_TYPE' property $name: ${property.format}")
                }
                STRING_TYPE -> visitor.visit(name, property.default as? String, property, required)
                OBJECT_TYPE -> visitor.visit(name, property.default as? Schema<*>, property, required, this)
                else -> error("Unsupported type of property")
            }
        }.onFailure {
            throw CodecException("Cannot parse field [$name] inside of schema with type ${property.type}", it)
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun processArrayProperty(property: ArraySchema, visitor: SchemaVisitor<*, *>, name: String, required: Boolean = false) {
        runCatching {
            when(property.items.type) {
                INTEGER_TYPE -> when (property.items.format) {
                    "int64" -> visitor.visitLongCollection(name, property.default as? List<Long>, property, required)
                    null, "", "int32" -> visitor.visitIntegerCollection(name, property.default as? List<Int>, property, required)
                    else -> error("Unsupported format of '$INTEGER_TYPE' property: ${property.format}")
                }
                BOOLEAN_TYPE -> visitor.visitBooleanCollection(name, property.default as? List<Boolean>, property, required)
                NUMBER_TYPE -> when (property.items.format) {
                    "float" -> visitor.visitFloatCollection(name, property.default as? List<Float>, property, required)
                    "double" -> visitor.visitDoubleCollection(name, property.default as? List<Double>, property, required)
                    null, "" -> visitor.visitStringCollection(name, property.default as? List<String>, property, required)
                    "-" -> visitor.visitBigDecimalCollection(name, property.default as? List<BigDecimal>, property, required)
                    else -> error("Unsupported format of '$NUMBER_TYPE' property: ${property.format}")
                }
                STRING_TYPE -> visitor.visitStringCollection(name, property.default as? List<String>, property, required)
                OBJECT_TYPE -> visitor.visitObjectCollection(name, property.default as? List<Any>, property, required, this)
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