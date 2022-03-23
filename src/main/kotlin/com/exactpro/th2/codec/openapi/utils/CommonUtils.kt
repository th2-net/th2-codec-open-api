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

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.getField
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.value.getString

/**
 * @param fieldName name of extracted field
 * @param required check if value is required, used only if extracted value was null
 */
fun Message.getField(fieldName: String, required: Boolean): Value? = this@getField.getField(fieldName).apply {
    if (required) checkNotNull(this) { "Field [$fieldName] is required for message [$messageType]" }
}

fun Value.getFloat(): Float? = this.getString()?.toFloatOrNull()
fun Value.getBoolean(): Boolean? = this.getString()?.toBooleanStrictOrNull()