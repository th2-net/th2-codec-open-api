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