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
import java.lang.IllegalStateException

sealed class SchemaVisitor<FromType, ToType> {
    abstract val openAPI: OpenAPI
    abstract val from: FromType
    abstract fun getResult(): ToType
    abstract fun visit(fieldName: String, fldStruct: ObjectSchema, required: Boolean, throwUndefined: Boolean = true)
    abstract fun visit(fieldName: String, fldStruct: ArraySchema, required: Boolean, throwUndefined: Boolean = true)
    abstract fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean)
    abstract fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean)
    abstract fun checkUndefined(objectSchema: ObjectSchema)
    abstract fun checkAgainst(fldStruct: ObjectSchema): Boolean

    fun oneOf(list: List<ObjectSchema>): List<ObjectSchema> = list.filter(this::checkAgainst).also {
        if (it.size != 1) {
            throw IllegalStateException("OneOf statement have ${it.size} valid schemas")
        }
    }

    fun anyOf(list: List<ObjectSchema>): List<ObjectSchema> = list.filter(this::checkAgainst).also{
        if (it.isEmpty()) {
            throw IllegalStateException("AnyOf statement had zero valid schemas")
        }
    }

    fun allOf(list: List<ObjectSchema>): List<ObjectSchema> =  list.filter(this::checkAgainst).also{
        if (list.size != it.size) {
            throw IllegalStateException("AllOf statement have only ${it.size} valid schemas of ${list.size} available")
        }
    }

    abstract class EncodeVisitor<T> : SchemaVisitor<T, ByteString>()

    abstract class DecodeVisitor<T> : SchemaVisitor<T, Message.Builder>()
}