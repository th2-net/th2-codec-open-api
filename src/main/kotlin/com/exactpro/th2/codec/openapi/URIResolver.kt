package com.exactpro.th2.codec.openapi

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.getString
import io.swagger.v3.oas.models.parameters.Parameter

class URIResolver {
    companion object {
        fun resolve(params: List<Parameter>, uriMessage: Message?, uriInput: String): String {
            var uri = uriInput
            if (uriMessage != null) {
                for (param in params) {
                    if (param.required && !uriMessage.containsFields(param.name)) error("Parameter [${param.name}] is required to be in [${OpenApiCodec.URI_PARAMS_FIELD}] field inside of message")
                    uri = uri.replaceParam(param.name, uriMessage.getString(param.name)?:"")
                }
            } else {
                params.firstOrNull { it.required }?.let {
                    error("Message must have $it param as required for $uriInput path")
                }
                for (param in params) {
                    uri = uri.replaceParam(param.name, "")
                }
            }
            return uri
        }

        fun String.replaceParam(name: String, value: Any) : String {
            return this.replace("[{]$name[}]".toRegex(), value.toString())
        }
    }
}