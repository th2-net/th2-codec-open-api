package com.exactpro.th2.codec.openapi.schemacontainer

import io.swagger.v3.oas.models.media.Schema

sealed interface HttpContainer {
    val body: Schema<*>?
}


