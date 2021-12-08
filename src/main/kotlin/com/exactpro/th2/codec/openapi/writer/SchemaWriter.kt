package com.exactpro.th2.codec.openapi.writer

import com.exactpro.th2.codec.openapi.findByRef
import com.exactpro.th2.codec.openapi.visitors.ISchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.getDouble
import com.exactpro.th2.common.message.getField
import com.exactpro.th2.common.message.getInt
import com.exactpro.th2.common.message.getLong
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
        msgStructure: Schema<*>,
        message: Message
    ) {
        if (msgStructure.`$ref` != null) {
            val schema = openApi.findByRef(msgStructure.`$ref` ) ?: error("Schema by ref ${msgStructure.`$ref`} wasn't found")
            traverse(schemaVisitor, schema, message)
        } else {
            msgStructure.properties.forEach { (name, property) ->
                if (msgStructure.required.contains(name)) {
                    requireNotNull(message.getField(name)) {"Message doesn't contain required field: $name"}
                }
                processProperty(property, schemaVisitor, name, message)
            }
        }

    }

    private fun processProperty(property: Schema<*>, visitor: ISchemaVisitor, name: String, message: Message) {
        when(property.type) {
            INTEGER_TYPE -> {
                visitor.visit(name, message.getInt(name), property)
            }
            BOOLEAN_TYPE -> {
                visitor.visit(name, message.getString(name)?.toBoolean(), property)
            }
            NUMBER_TYPE -> {
                when (property.format) {
                    "float" -> {
                        visitor.visit(name, message.getField(name)?.getString()?.toFloat(), property)
                    }
                    "int64" -> {
                        visitor.visit(name, message.getLong(name), property)
                    }
                    "double" -> {
                        visitor.visit(name, message.getDouble(name), property)
                    }
                    null, "", "int32" -> {
                        visitor.visit(name, message.getInt(name), property)
                    }
                    else -> {
                        error("Unsupported format of property $name: ${property.format}")
                    }
                }
            }
            STRING_TYPE -> {
                message.getString(name).let { value ->
                    visitor.visit(name, value, property)
                    checkEnum(property, value, name)
                }
            }
            OBJECT_TYPE -> {

            }
            null -> {
                val ref = property.`$ref` ?: error("Unsupported type of property $name: null")
                val propertyFromRef = openApi.findByRef(ref) ?: error("Schema by ref $ref wasn't found")
                processProperty(propertyFromRef, visitor, name, message)
            }
        }
    }

    private fun <T>checkEnum(property: Schema<*>, value: T, name: String) {
        if (property.enum != null && property.enum.size > 0 && !(property.enum).contains(value)) {
            error("Enum list of property $name doesn't contain $value")
        }
    }
}