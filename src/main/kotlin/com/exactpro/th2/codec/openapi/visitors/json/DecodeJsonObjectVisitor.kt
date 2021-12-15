package com.exactpro.th2.codec.openapi.visitors.json

import com.exactpro.th2.codec.openapi.utils.checkEnum
import com.exactpro.th2.codec.openapi.utils.getArray
import com.exactpro.th2.codec.openapi.utils.getRequiredField
import com.exactpro.th2.codec.openapi.utils.putAll
import com.exactpro.th2.codec.openapi.visitors.ISchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.value.getList
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class DecodeJsonObjectVisitor(jsonString: String) : ISchemaVisitor<Message> {
    private val rootMessage = message()
    private val json = mapper.readTree(jsonString)

    override fun visit(
        fieldName: String,
        defaultValue: Schema<*>?,
        fldStruct: Schema<*>,
        references: OpenAPI,
        required: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asText()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asBoolean()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asInt()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asText()?.toFloat()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asDouble()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean) {
        val value = json.getRequiredField(fieldName, required)?.asLong()
        fldStruct.checkEnum(value, fieldName)
        rootMessage.addFields(fieldName, value ?: defaultValue)
    }

    override fun visitBooleanCollection(
        fieldName: String,
        defaultValue: List<Boolean>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.map { it.asBoolean() }?.let {
            rootMessage.addField(fieldName, it)
        }
    }

    override fun visitIntegerCollection(
        fieldName: String,
        defaultValue: List<Int>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.map { it.asInt() }?.let {
            rootMessage.addField(fieldName, it)
        }
    }

    override fun visitStringCollection(
        fieldName: String,
        defaultValue: List<String>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.map { it.asText() }?.let {
            rootMessage.addField(fieldName, it)
        }
    }

    override fun visitDoubleCollection(
        fieldName: String,
        defaultValue: List<Double>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.map { it.asDouble() }?.let {
            rootMessage.addField(fieldName, it)
        }
    }

    override fun visitFloatCollection(
        fieldName: String,
        defaultValue: List<Float>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.map { it.asText().toFloat() }?.let {
            rootMessage.addField(fieldName, it)
        }
    }

    override fun visitLongCollection(
        fieldName: String,
        defaultValue: List<Long>?,
        fldStruct: Schema<*>,
        required: Boolean
    ) {
        json.getArray(fieldName, required)?.map { it.asLong() }?.let {
            rootMessage.addField(fieldName, it)
        }
    }

    override fun getResult(): Message = rootMessage.build()

    private companion object {
        val mapper = ObjectMapper()
    }

}