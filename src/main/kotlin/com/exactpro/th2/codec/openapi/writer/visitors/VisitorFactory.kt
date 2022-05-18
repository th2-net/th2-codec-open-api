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

package com.exactpro.th2.codec.openapi.writer.visitors

import com.exactpro.th2.codec.openapi.utils.JsonSchemaTypes
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonObjectVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonObjectVisitor
import com.exactpro.th2.common.grpc.Message
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema

object VisitorFactory {
    private const val JSON_FORMAT = "application/json"


    fun createEncodeVisitor(format: String, schema: Schema<*>, message: Message, dictionary: OpenAPI): SchemaVisitor.EncodeVisitor<*> {
        when (format) {
            JSON_FORMAT -> {
                return when (schema.defineType(dictionary)) {
                    JsonSchemaTypes.ARRAY -> EncodeJsonArrayVisitor(message)
                    JsonSchemaTypes.OBJECT -> EncodeJsonObjectVisitor(message)
                }
            }
            else -> error("Unsupported format of message $format for encode")
        }
    }

    fun createDecodeVisitor(format: String, schema: Schema<*>, data: ByteString, dictionary: OpenAPI): SchemaVisitor.DecodeVisitor<*> {
        when (format) {
            JSON_FORMAT -> {
                return when (schema.defineType(dictionary)) {
                    JsonSchemaTypes.ARRAY -> DecodeJsonArrayVisitor(data.toStringUtf8())
                    JsonSchemaTypes.OBJECT -> DecodeJsonObjectVisitor(data.toStringUtf8())
                }
            }
            else -> error("Unsupported format of message $format for decode")
        }
    }

    private fun Schema<*>.defineType(dictionary: OpenAPI): JsonSchemaTypes {
        return when (this) {
            is ArraySchema -> JsonSchemaTypes.ARRAY
            is ObjectSchema -> JsonSchemaTypes.OBJECT
            is ComposedSchema -> {
                val schemas = when {
                    !this.anyOf.isNullOrEmpty() -> this.anyOf
                    !this.oneOf.isNullOrEmpty() -> this.oneOf
                    !this.allOf.isNullOrEmpty() -> this.allOf
                    else -> error("Unsupported state of composed schema, [ anyOf | oneOf | allOf ] list is empty")
                }
                when (dictionary.getEndPoint(schemas.first())) {
                    is ArraySchema -> JsonSchemaTypes.ARRAY
                    is ObjectSchema -> JsonSchemaTypes.OBJECT
                    is ComposedSchema -> error("Unsupported level of abstraction, cannot parse composed scheme inside of composed scheme")
                    //TODO: More than one level of abstraction isn't impossible, need to implement later, this is fast try of realization
                    else -> error("Unsupported type of first element from composed schema [${schemas.first()::class.simpleName}], array or object required")
                }
            }
            else -> error("Unsupported type of json schema [${this::class.simpleName}], array or object required")
        }
    }

}