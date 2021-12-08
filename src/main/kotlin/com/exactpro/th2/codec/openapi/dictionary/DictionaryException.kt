package com.exactpro.th2.codec.openapi.dictionary

import com.exactpro.th2.codec.CodecException
import org.openapitools.codegen.validation.Invalid

class DictionaryException(message: String, errors: List<Invalid>, additionalErrors: List<String>) : CodecException(
    buildString {
        append(message)
        append("\nIssues: ")
        append(errors.joinToString { "[ERROR: ${it.message}|${it.details}], " })
        append(additionalErrors.joinToString { "[ERROR: $it], " })
    }
)