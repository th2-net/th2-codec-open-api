package com.exactpro.th2.codec.openapi.visitors

import com.exactpro.sf.common.messages.structures.IFieldStructure
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.oas.models.media.Schema


class JsonVisitor : ISchemaVisitor {
    var mapper = ObjectMapper()
    val rootNode: ObjectNode = mapper.createObjectNode()

    override fun visit(fieldName: String, value: Schema<*>, fldStruct: Schema<*>) {
        TODO("Not yet implemented")
    }

    override fun visit(fieldName: String, value: String?, fldStruct: Schema<*>) {
        rootNode.put(fieldName, value)
    }

    override fun visit(fieldName: String, value: Boolean?, fldStruct: Schema<*>) {
        rootNode.put(fieldName, value)
    }

    override fun visit(fieldName: String, value: Int?, fldStruct: Schema<*>) {
        rootNode.put(fieldName, value)
    }

    override fun visit(fieldName: String, value: Float?, fldStruct: Schema<*>) {
        rootNode.put(fieldName, value)
    }

    override fun visit(fieldName: String, value: Double?, fldStruct: Schema<*>) {
        rootNode.put(fieldName, value)
    }

    override fun visit(fieldName: String, value: Long?, fldStruct: Schema<*>) {
        rootNode.put(fieldName, value)
    }
}
