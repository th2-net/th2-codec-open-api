/*
 * Copyright 2021-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.codec.openapi.writer.visitors.json

import com.exactpro.th2.common.grpc.Message
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

class EncodeJsonArrayVisitor(from: Message, openAPI: OpenAPI) : EncodeJsonObjectVisitor(from, openAPI) {
    override fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun visit(fieldName: String, fldStruct: Schema<*>, required: Boolean, throwUndefined: Boolean) = throw UnsupportedOperationException("Array visitor supports only collections")

    override fun getFieldNames() = throw UnsupportedOperationException("Array visitor supports only collections")
    override fun getResult(): ByteString = ByteString.copyFrom(rootNode.first().toString().toByteArray())
}