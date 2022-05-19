package com.exactpro.th2.codec.openapi.writer.visitors.json.updated

import com.exactpro.th2.codec.openapi.utils.checkEnum
import com.exactpro.th2.codec.openapi.utils.getField
import com.exactpro.th2.codec.openapi.utils.validateAsBigDecimal
import com.exactpro.th2.codec.openapi.utils.validateAsBoolean
import com.exactpro.th2.codec.openapi.utils.validateAsLong
import com.exactpro.th2.codec.openapi.writer.visitors.UpdatedSchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

class DecodeJsonObjectVisitor(override val from: ObjectNode, override val openAPI: OpenAPI) : UpdatedSchemaVisitor.DecodeVisitor<ObjectNode>() {
    private val rootMessage = message()

    constructor(jsonString: String, openAPI: OpenAPI) : this(mapper.readTree(jsonString) as ObjectNode, openAPI)

    override fun visit(fieldName: String, fldStruct: ObjectSchema, required: Boolean, checkUndefined: Boolean) {
        TODO("Not yet implemented")
    }

    override fun visit(fieldName: String, fldStruct: ArraySchema, required: Boolean) {
        TODO("Not yet implemented")
    }

    override fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean) {
        TODO("Not yet implemented")
    }

    override fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required)?.validateAsBigDecimal(), fldStruct)
    override fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required)?.validateAsLong(), fldStruct)
    override fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required)?.asText(), fldStruct)
    override fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required)?.validateAsBoolean(), fldStruct)

    private fun <T> visitPrimitive(fieldName: String, value: T?, fldStruct: Schema<T>) {
        (value?.also { fldStruct.checkEnum(it, fieldName) } ?: fldStruct.default)?.let {
            rootMessage.addFields(fieldName, it)
        }
    }

    override fun getResult(): Message.Builder {
        TODO("Not yet implemented")
    }

    private companion object {
        val mapper = ObjectMapper().apply {
            nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
        }
    }
}