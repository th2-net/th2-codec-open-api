package com.exactpro.th2.codec.openapi.writer.visitors

import com.exactpro.th2.codec.openapi.schemacontainer.HttpContainer
import com.exactpro.th2.codec.openapi.utils.JsonSchemaTypes
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonObjectVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonObjectVisitor
import com.exactpro.th2.common.grpc.Message
import com.google.protobuf.ByteString

object VisitorFactory {
    private const val JSON_FORMAT = "application/json"

    fun createEncodeVisitor(format: String, type: String, message: Message): SchemaVisitor.EncodeVisitor<*> {
        when (format) {
            JSON_FORMAT -> {
                return when (JsonSchemaTypes.getType(type)) {
                    JsonSchemaTypes.ARRAY -> EncodeJsonArrayVisitor(message)
                    JsonSchemaTypes.OBJECT -> EncodeJsonObjectVisitor(message)
                    else -> error("Unsupported type of json schema [$type], array or object required")
                }
            }
            else -> error("Unsupported format of message $format")
        }
    }

    fun createDecodeVisitor(format: String, type: String, data: ByteString): SchemaVisitor.DecodeVisitor<*> {
        when (format) {
            JSON_FORMAT -> {
                return when (JsonSchemaTypes.getType(type)) {
                    JsonSchemaTypes.ARRAY -> DecodeJsonArrayVisitor(data.toStringUtf8())
                    JsonSchemaTypes.OBJECT -> DecodeJsonObjectVisitor(data.toStringUtf8())
                    else -> error("Unsupported type of json schema [$type], array or object required")
                }
            }
            else -> error("Unsupported format of message $format")
        }
    }
}