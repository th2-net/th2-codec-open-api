package com.exactpro.th2.codec.openapi.visitors

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.getList
import com.exactpro.th2.common.value.getDouble
import com.exactpro.th2.common.value.getInt
import com.exactpro.th2.common.value.getLong
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

    override fun visit(
        fieldName: String,
        defaultValue: Schema<*>?,
        fldStruct: Schema<*>,
        references: OpenAPI,
        required: Boolean
    ) {
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

    override fun visitBooleanCollection(
        fieldName: String,
        defaultValue: List<Boolean>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getList(fieldName)?.forEach {
            rootNode.add(it.getString().toBoolean())
        } ?: defaultValue?.forEach {
            rootNode.add(it)
        }
    }

    override fun visitIntegerCollection(
        fieldName: String,
        defaultValue: List<Int>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getList(fieldName)?.forEach {
            rootNode.add(it.getInt())
        } ?: defaultValue?.forEach {
            rootNode.add(it)
        }
    }

    override fun visitStringCollection(
        fieldName: String,
        defaultValue: List<String>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getList(fieldName)?.forEach {
            rootNode.add(it.getString())
        } ?: defaultValue?.forEach {
            rootNode.add(it)
        }
    }

    override fun visitDoubleCollection(
        fieldName: String,
        defaultValue: List<Double>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getList(fieldName)?.forEach {
            rootNode.add(it.getDouble())
        } ?: defaultValue?.forEach {
            rootNode.add(it)
        }
    }

    override fun visitFloatCollection(
        fieldName: String,
        defaultValue: List<Float>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getList(fieldName)?.forEach {
            rootNode.add(it.getString()?.toFloat())
        } ?: defaultValue?.forEach {
            rootNode.add(it)
        }
    }

    override fun visitLongCollection(
        fieldName: String,
        defaultValue: List<Long>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        message.getList(fieldName)?.forEach {
            rootNode.add(it.getLong())
        } ?: defaultValue?.forEach {
            rootNode.add(it)
        }
    }

    override fun getResult(): String {
        return rootNode.toPrettyString()
    }
}