package com.exactpro.th2.codec.openapi.writer.visitors.json

import com.exactpro.th2.common.grpc.Message
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

class EncodeJsonArrayVisitor(from: Message, openAPI: OpenAPI) : EncodeJsonObjectVisitor(from, openAPI) {
    override fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: Schema<*>, required: Boolean, throwUndefined: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun checkUndefined(objectSchema: Schema<*>) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun checkAgainst(fldStruct: ObjectSchema) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun getResult(): ByteString = ByteString.copyFrom(rootNode.first().toString().toByteArray())
}