package com.exactpro.th2.codec.openapi.http

import io.swagger.v3.oas.models.media.Schema

sealed interface HttpContainer {
    abstract val body: Schema<*>?
}


