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

package com.exactpro.th2.codec.openapi.writer

import com.exactpro.th2.codec.openapi.utils.ARRAY_TYPE
import com.exactpro.th2.codec.openapi.utils.getEndPoint
import com.exactpro.th2.codec.openapi.utils.requiredContains
import com.exactpro.th2.codec.openapi.writer.visitors.SchemaVisitor
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ByteArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.EmailSchema
import io.swagger.v3.oas.models.media.FileSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.PasswordSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.UUIDSchema
import java.lang.IllegalStateException


class SchemaWriter constructor(private val openApi: OpenAPI, private val throwUndefined: Boolean = true) {

    fun traverse(
        visitor: SchemaVisitor<*, *>,
        msgStructure: Schema<*>
    ) {
        when (msgStructure) {
            is ArraySchema -> visitor.visit(ARRAY_TYPE, msgStructure, true)
            is ComposedSchema -> {
                when {
                    !msgStructure.allOf.isNullOrEmpty() -> visitor.allOf(msgStructure.allOf.map { openApi.getEndPoint(it) } as List<ObjectSchema>).forEach {
                        traverse(visitor, it)
                    }
                    !msgStructure.oneOf.isNullOrEmpty() -> visitor.oneOf(msgStructure.oneOf.map { openApi.getEndPoint(it) } as List<ObjectSchema>).forEach {
                        traverse(visitor, it)
                    }
                    !msgStructure.anyOf.isNullOrEmpty() -> visitor.anyOf(msgStructure.anyOf.map { openApi.getEndPoint(it) } as List<ObjectSchema>).forEach {
                        traverse(visitor, it)
                    }
                    else -> throw IllegalStateException("Composed schema was empty at allOf, oneOf, anyOf lists")
                }
            }
            else -> {
                if (throwUndefined) {
                    visitor.checkUndefined(msgStructure)
                }
                msgStructure.properties.forEach { (name, property) ->
                    when(property) {
                        is ArraySchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is StringSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is IntegerSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is NumberSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is BooleanSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is ComposedSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is BinarySchema, is ByteArraySchema, is DateSchema, is DateTimeSchema, is EmailSchema, is FileSchema, is MapSchema, is PasswordSchema, is UUIDSchema -> throw UnsupportedOperationException("${property::class.simpleName} isn't supported for now")
                        else -> visitor.visit(name, property, msgStructure.requiredContains(name), throwUndefined)
                    }
                }
            }
        }
    }
}