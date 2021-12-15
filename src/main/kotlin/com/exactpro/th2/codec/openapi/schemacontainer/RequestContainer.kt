package com.exactpro.th2.codec.openapi.schemacontainer

import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter

class RequestContainer(val path: String, val method: String, val params: List<Parameter>?, override val body: Schema<*>?) : HttpContainer {

}