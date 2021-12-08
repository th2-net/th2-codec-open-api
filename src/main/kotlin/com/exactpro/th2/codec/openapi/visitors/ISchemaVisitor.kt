package com.exactpro.th2.codec.openapi.visitors

import com.exactpro.sf.common.messages.structures.IFieldStructure
import com.exactpro.th2.common.grpc.Message
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

interface ISchemaVisitor {
    fun visit(fieldName: String, defaultValue: Schema<*>?, fldStruct: Schema<*>, references: OpenAPI, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: String?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Boolean?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Int?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Float?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Double?, fldStruct: Schema<*>, required: Boolean = false)
    fun visit(fieldName: String, defaultValue: Long?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitBooleanCollection(fieldName: String, defaultValue: List<Boolean>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitIntegerCollection(fieldName: String, defaultValue: List<Int>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitStringCollection(fieldName: String, defaultValue: List<String>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitDoubleCollection(fieldName: String, defaultValue: List<Double>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitFloatCollection(fieldName: String, defaultValue: List<Float>?, fldStruct: Schema<*>, required: Boolean = false)
    fun visitLongCollection(fieldName: String, defaultValue: List<Long>?, fldStruct: Schema<*>, required: Boolean = false)

    fun getResult(): String

}