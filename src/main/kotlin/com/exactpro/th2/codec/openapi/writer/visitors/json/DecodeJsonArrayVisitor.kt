package com.exactpro.th2.codec.openapi.writer.visitors.json

import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.validateAsBigDecimal
import com.exactpro.th2.codec.openapi.utils.validateAsBoolean
import com.exactpro.th2.codec.openapi.utils.validateAsLong
import com.exactpro.th2.codec.openapi.utils.validateAsObject
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

class DecodeJsonArrayVisitor(override val from: JsonNode, override val openAPI: OpenAPI) : SchemaVisitor.DecodeVisitor<JsonNode>() {

    constructor(jsonString: String, openAPI: OpenAPI) : this(mapper.readTree(jsonString), openAPI)

    internal val rootMessage = message()
    private val fromArray = from as ArrayNode

    override fun visit(fieldName: String, fldStruct: ArraySchema, required: Boolean, throwUndefined: Boolean) {
        when (val itemSchema = openAPI.getEndPoint(fldStruct.items)) {
            is NumberSchema -> rootMessage.addField(fieldName, fromArray.map { it.validateAsBigDecimal() })
            is IntegerSchema -> rootMessage.addField(fieldName, fromArray.map { it.validateAsLong() })
            is BooleanSchema -> rootMessage.addField(fieldName, fromArray.map { it.validateAsBoolean() })
            is StringSchema -> rootMessage.addField(fieldName, fromArray.map { it.asText() })
            is ObjectSchema -> rootMessage.addField(fieldName,  mutableListOf<Message>().apply {
                fromArray.forEach {
                    DecodeJsonObjectVisitor(checkNotNull(it.validateAsObject()) { " Value from list [$fieldName] must be message" }, openAPI).let { visitor ->
                        SchemaWriter(openAPI).traverse(visitor, itemSchema, throwUndefined)
                        visitor.rootMessage.build().run(this::add)
                    }
                }
            })
            is ComposedSchema -> {
                TODO("Not yet implemented")
            }
            else -> error("Unsupported items type: ${fldStruct.items::class.java}")
        }
    }

    override fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: Schema<*>, required: Boolean, throwUndefined: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun checkUndefined(objectSchema: Schema<*>) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun checkAgainst(fldStruct: ObjectSchema) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun getResult(): Message.Builder = rootMessage


    private companion object {
        val mapper = ObjectMapper().apply {
            nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
        }
    }
}