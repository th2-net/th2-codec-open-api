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

package com.exactpro.th2.codec.openapi.writer

import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.visitors.ISchemaVisitor
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.util.SchemaTypeUtil.INTEGER_TYPE
import io.swagger.v3.parser.util.SchemaTypeUtil.BOOLEAN_TYPE
import io.swagger.v3.parser.util.SchemaTypeUtil.NUMBER_TYPE
import io.swagger.v3.parser.util.SchemaTypeUtil.STRING_TYPE
import io.swagger.v3.parser.util.SchemaTypeUtil.OBJECT_TYPE


class SchemaWriter(val openApi: OpenAPI) {

    fun traverse(
        schemaVisitor: ISchemaVisitor,
        msgStructure: Schema<*>
    ) {
        val schema = openApi.getEndPoint(msgStructure)

        when (schema.type) {
            ARRAY_TYPE -> {
                processProperty(schema, schemaVisitor, ARRAY_TYPE)
            }
            OBJECT_TYPE -> {
                requireNotNull(schema.properties) {"Properties in object are required: $schema"}
                schema.properties.forEach { (name, property) ->
                    processProperty(openApi.getEndPoint(property), schemaVisitor, name, schema.required.contains(name))
                }
            }
        }
    }

    private fun processProperty(property: Schema<*>, visitor: ISchemaVisitor, name: String, required: Boolean = false) {
        when(property.type) {
            ARRAY_TYPE -> {
                processArrayProperty(property as ArraySchema, visitor, name, required)
            }
            INTEGER_TYPE -> {
                visitor.visit(name, property.default as? Int, property, required)
            }
            BOOLEAN_TYPE -> {
                visitor.visit(name, property.default as? Boolean, property, required)
            }
            NUMBER_TYPE -> {
                when (property.format) {
                    "float" -> {
                        visitor.visit(name, property.default as? Float, property, required)
                    }
                    "int64" -> {
                        visitor.visit(name, property.default as? Long, property, required)
                    }
                    "double" -> {
                        visitor.visit(name, property.default as? Double, property, required)
                    }
                    null, "", "int32" -> {
                        visitor.visit(name, property.default as? Int, property, required)
                    }
                    else -> {
                        error("Unsupported format of property $name: ${property.format}")
                    }
                }
            }
            STRING_TYPE -> {
                visitor.visit(name, property.default as? String, property, required)
            }
            OBJECT_TYPE -> {
                visitor.visit(name, property.default as? Schema<*>, property, openApi, required)
            }
            else -> error("Unsupported type of property $name: null")
        }
    }

    private fun processArrayProperty(property: ArraySchema, visitor: ISchemaVisitor, name: String, required: Boolean = false) {
        when(property.items.type) {
            INTEGER_TYPE -> {
                visitor.visitIntegerCollection(name, property.default as? List<Int>, property, required)
            }
            BOOLEAN_TYPE -> {
                visitor.visitBooleanCollection(name, property.default as? List<Boolean>, property, required)
            }
            NUMBER_TYPE -> {
                when (property.items.format) {
                    "float" -> {
                        visitor.visitFloatCollection(name, property.default as? List<Float>, property, required)
                    }
                    "int64" -> {
                        visitor.visitLongCollection(name, property.default as? List<Long>, property, required)
                    }
                    "double" -> {
                        visitor.visitDoubleCollection(name, property.default as? List<Double>, property, required)
                    }
                    null, "", "int32" -> {
                        visitor.visitIntegerCollection(name, property.default as? List<Int>, property, required)
                    }
                    else -> {
                        error("Unsupported format of property $name: ${property.format}")
                    }
                }
            }
            STRING_TYPE -> {
                visitor.visitStringCollection(name, property.default as? List<String>, property, required)
            }
            else -> error("Unsupported type of property $name: null")
        }
    }

    companion object {
        const val ARRAY_TYPE = "array"
    }
}