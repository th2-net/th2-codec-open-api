package com.exactpro.th2.codec.openapi.writer.visitors

import com.exactpro.th2.common.grpc.Message
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema

sealed class UpdatedSchemaVisitor<FromType, ToType> {
    abstract val openAPI: OpenAPI
    abstract val from: FromType
    abstract fun getResult(): ToType
    abstract fun visit(fieldName: String, fldStruct: ObjectSchema, required: Boolean, throwUndefined: Boolean = true)
    abstract fun visit(fieldName: String, fldStruct: ArraySchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean)

    abstract class EncodeVisitor<T> : UpdatedSchemaVisitor<T, ByteString>()

    abstract class DecodeVisitor<T> : UpdatedSchemaVisitor<T, Message.Builder>()
}