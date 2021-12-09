package com.exactpro.th2.codec.openapi.utils

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.getField
import com.exactpro.th2.common.message.messageType

fun Message.getRequiredField(fieldName: String, required: Boolean): Value? {
    val result = getField(fieldName)
    return if (required) {
        checkNotNull(result) {"Field [$fieldName] is required for message [$messageType]"}
    } else result
}