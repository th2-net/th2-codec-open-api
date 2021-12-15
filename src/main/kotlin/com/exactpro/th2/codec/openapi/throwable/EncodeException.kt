package com.exactpro.th2.codec.openapi.throwable

import com.exactpro.th2.codec.CodecException

class EncodeException(msg: String, throwable: Throwable) : CodecException(msg, throwable) {
}