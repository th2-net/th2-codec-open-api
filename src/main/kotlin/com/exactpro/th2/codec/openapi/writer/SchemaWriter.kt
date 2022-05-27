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
import io.swagger.v3.oas.models.media.PasswordSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.UUIDSchema


class SchemaWriter constructor(private val openApi: OpenAPI) {

    fun traverse(
        visitor: SchemaVisitor<*, *>,
        msgStructure: Schema<*>,
        checkForUndefinedFields: Boolean = true
    ) {
        when (msgStructure) {
            is ArraySchema -> visitor.visit(ARRAY_TYPE, msgStructure, true)
            is ComposedSchema -> {
                when {
                    !msgStructure.allOf.isNullOrEmpty() -> visitor.allOf(msgStructure.allOf.map(openApi::getEndPoint)).forEach {
                        traverse(visitor, it, false)
                    }
                    !msgStructure.oneOf.isNullOrEmpty() -> visitor.oneOf(msgStructure.oneOf.map(openApi::getEndPoint)).also {
                        traverse(visitor, it, false)
                    }
                    !msgStructure.anyOf.isNullOrEmpty() -> visitor.anyOf(msgStructure.anyOf.map(openApi::getEndPoint)).forEach {
                        traverse(visitor, it, false)
                    }
                    else -> error("Composed schema has no allOf, oneOf, anyOf definitions")
                }
            }
            else -> {
                if (checkForUndefinedFields) {
                    visitor.checkUndefined(msgStructure)
                }
                msgStructure.properties.forEach { (name, property) ->
                    when(property) {
                        is ArraySchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is StringSchema -> visitor.visit(name, property , msgStructure.requiredContains(name))
                        is IntegerSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is NumberSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is BooleanSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is DateSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is DateTimeSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is ComposedSchema -> visitor.visit(name, property, msgStructure.requiredContains(name))
                        is PasswordSchema, is EmailSchema, is BinarySchema, is ByteArraySchema, is FileSchema, is MapSchema, is UUIDSchema -> throw UnsupportedOperationException("${property::class.simpleName} isn't supported for now")
                        else -> visitor.visit(name, property, msgStructure.requiredContains(name), checkForUndefinedFields)
                    }
                }
            }
        }
    }
}