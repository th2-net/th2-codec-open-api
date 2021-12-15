package com.exactpro.th2.codec.openapi.schemacontainer

import io.swagger.v3.oas.models.media.Schema

class ResponseContainer(val path: String, val method: String, val code: String, override val body: Schema<*>?) : HttpContainer {

}