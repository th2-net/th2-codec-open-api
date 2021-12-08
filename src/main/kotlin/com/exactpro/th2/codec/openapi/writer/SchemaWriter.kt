package com.exactpro.th2.codec.openapi.writer

import com.exactpro.th2.codec.openapi.getEndPoint
import com.exactpro.th2.codec.openapi.visitors.ISchemaVisitor
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.value.getString
import io.swagger.v3.oas.models.OpenAPI
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
                    processProperty(openApi.getEndPoint(property), schemaVisitor, name)
                }
            }
        }
    }

    private fun processProperty(property: Schema<*>, visitor: ISchemaVisitor, name: String) {
        when(property.type) {
            ARRAY_TYPE -> {
                error("Unsupported type right now")
            }
            INTEGER_TYPE -> {
                visitor.visit(name, property.default as? Int, property)
            }
            BOOLEAN_TYPE -> {
                visitor.visit(name, property.default as? Boolean, property)
            }
            NUMBER_TYPE -> {
                when (property.format) {
                    "float" -> {
                        visitor.visit(name, property.default as? Float, property)
                    }
                    "int64" -> {
                        visitor.visit(name, property.default as? Long, property)
                    }
                    "double" -> {
                        visitor.visit(name, property.default as? Double, property)
                    }
                    null, "", "int32" -> {
                        visitor.visit(name, property.default as? Int, property)
                    }
                    else -> {
                        error("Unsupported format of property $name: ${property.format}")
                    }
                }
            }
            STRING_TYPE -> {
                visitor.visit(name, property.default as? String, property)
            }
            OBJECT_TYPE -> {
                visitor.visit(name, property.default as? Schema<*>, property, openApi)
            }
            null -> error("Unsupported type of property $name: null")
        }
    }

    companion object {
        const val ARRAY_TYPE = "array"
    }
}