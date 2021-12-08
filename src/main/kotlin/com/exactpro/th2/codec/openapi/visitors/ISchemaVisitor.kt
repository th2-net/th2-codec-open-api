package com.exactpro.th2.codec.openapi.visitors

import io.swagger.v3.oas.models.media.Schema

interface ISchemaVisitor {
    fun visit(fieldName: String, value: Schema<*>, fldStruct: Schema<*>)
    fun visit(fieldName: String, value: String?, fldStruct: Schema<*>)
    fun visit(fieldName: String, value: Boolean?, fldStruct: Schema<*>)
    fun visit(fieldName: String, value: Int?, fldStruct: Schema<*>)
    fun visit(fieldName: String, value: Float?, fldStruct: Schema<*>)
    fun visit(fieldName: String, value: Double?, fldStruct: Schema<*>)
    fun visit(fieldName: String, value: Long?, fldStruct: Schema<*>)
}