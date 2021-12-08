package com.exactpro.th2.codec.openapi

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

enum class MessageFormat(val format: String) {
    JSON("application/json");

    companion object {
        fun getFormat(format: String) : MessageFormat? {
            values().forEach {
                if (it.format == format) return it
            }
            return null
        }
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