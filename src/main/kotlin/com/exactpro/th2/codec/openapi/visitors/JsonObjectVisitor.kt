package com.exactpro.th2.codec.openapi.visitors

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.getDouble
import com.exactpro.th2.common.message.getInt
import com.exactpro.th2.common.message.getLong
import com.exactpro.th2.common.message.getString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema


class JsonObjectVisitor(val message: Message) : ISchemaVisitor {
    private val rootNode: ObjectNode = mapper.createObjectNode()

    override fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, references: OpenAPI, required: Boolean) {
        TODO("Not yet implemented")
    }

    override fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean) {
        rootNode.put(fieldName, message.getString(fieldName) ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean) {
        rootNode.put(fieldName, message.getString(fieldName)?.toBoolean() ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean) {
        rootNode.put(fieldName, message.getInt(fieldName) ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean) {
        rootNode.put(fieldName, message.getString(fieldName)?.toFloat() ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean) {
        rootNode.put(fieldName, message.getDouble(fieldName) ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean) {
        rootNode.put(fieldName, message.getLong(fieldName) ?: defaultValue)
    }

    override fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: Schema<*>, required: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getResult(): String {
        return rootNode.toPrettyString()
    }

    private fun <T>checkEnum(property: Schema<*>, value: T, name: String) {
        if (property.enum != null && property.enum.size > 0 && !(property.enum).contains(value)) {
            error("Enum list of property $name doesn't contain $value")
        }
    }

    companion object {
        private var mapper = ObjectMapper()
    }
}
