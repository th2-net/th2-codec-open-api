package com.exactpro.th2.codec.openapi.visitors

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.getList
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class JsonArrayVisitor(val message: Message) : ISchemaVisitor {
    private val rootNode: ArrayNode = mapper.createArrayNode()

    companion object {
        private var mapper = ObjectMapper()
    }

    override fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, references: OpenAPI, required: Boolean) {
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

    override fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: Schema<*>, required: Boolean) {
        message.getList(fieldName)?.forEach {
            rootNode.add(it.getString().toBoolean())
        } ?: defaultValue?.forEach {
            rootNode.add(it)
        }
    }

    override fun getResult(): String {
        return rootNode.toPrettyString()
    }
}