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

package com.exactpro.th2.codec.openapi.utils

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.models.RefType

private fun PathItem.getByMethod(method: String) : Operation? {
    when (method.lowercase()) {
        "get" -> {
            return get
        }
        "put" -> {
            return put
        }
        "delete" -> {
            return delete
        }
        "post" -> {
            return post
        }
        else -> {
            error("Unsupported method inside message")
        }
    }
}

fun PathItem.getMethods() : Map<String, Operation> {
    return listOf("get", "put", "delete", "post").map {
        it to getByMethod(it)
    }.filter { it.second != null }.toMap() as Map<String, Operation>
}

fun OpenAPI.findByRef(ref: String) : Schema<*>? {
    if (ref.startsWith(RefType.SCHEMAS.internalPrefix)) {
        return this.components.schemas[ref.drop(RefType.SCHEMAS.internalPrefix.length)]
    } else error("Unsupported ref type: $ref")

}

fun OpenAPI.getEndPoint(schema: Schema<*>): Schema<*> {
    return if (schema.`$ref` != null) {
        findByRef(schema.`$ref`) ?: error("Unsupported schema, no reference was found: ${schema.`$ref`}")
    } else {
        schema
    }
}

enum class JsonSchemaTypes(val type: String) {
    ARRAY("array"), OBJECT("object");

    companion object {
        fun getType(type: String) : JsonSchemaTypes? {
            values().forEach {
                if (it.type == type) return it
            }
            return null
        }
    }
}

fun <T>Schema<*>.checkEnum(value: T?, name: String) {
    if (value != null && enum != null && enum.size > 0 && !enum.contains(value)) {
        error("Enum list of property $name doesn't contain $value")
    }
}