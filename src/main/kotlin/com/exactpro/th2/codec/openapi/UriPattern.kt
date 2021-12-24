package com.exactpro.th2.codec.openapi

import io.swagger.v3.oas.models.parameters.Parameter
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

class UriPattern(val pattern: String) {
    private val pathMatcher: Regex
    private val queryParamMatchers: Map<String, Regex>

    init {
        val uri = URI(pattern.replace(PARAM_MATCHER, PARAM_PLACEHOLDER))

        pathMatcher = uri.path.urlDecode().replace(PARAM_PLACEHOLDER, ANY_VALUE_PATTERN).toRegex()
        queryParamMatchers = uri.query?.run {
            split(PARAM_SEPARATOR)
                .asSequence()
                .map { it.split(NAME_VALUE_SEPARATOR, limit = 2) }
                .associate { (name, value) ->
                    name to when (value) {
                        PARAM_PLACEHOLDER -> ANY_VALUE_PATTERN
                        else -> Regex.escape(value.urlDecode())
                    }.toRegex()
                }
        } ?: emptyMap()
    }

    /**
     * Checks if the provided [uri] matches this URI pattern
     */
    fun matches(uri: String): Boolean {
        val parsedUri = URI(uri)

        if (!pathMatcher.matches(parsedUri.path.urlDecode())) {
            return false
        }

        val uriQueryParams = parsedUri.query?.run {
            split(PARAM_SEPARATOR)
                .map { it.split(NAME_VALUE_SEPARATOR, limit = 2) }
                .associate { (name, value) -> name to value.urlDecode() }
        } ?: emptyMap()

        if (!queryParamMatchers.keys.containsAll(uriQueryParams.keys)) {
            return false
        }

        queryParamMatchers.forEach { (paramName, paramMatcher) ->
            val paramValue = uriQueryParams[paramName] ?: return false

            if (!paramMatcher.matches(paramValue)) {
                return false
            }
        }

        return true
    }

    /**
     * Resolves this URI pattern by substituting parameter placeholders with values from [paramValues]
     */
    fun resolve(paramsFromSchema: Map<String, Parameter>, paramValues: Map<String, String>): String {
        return PARAM_MATCHER.replace(pattern) {
            if (paramValues.isEmpty()) {
                paramsFromSchema.values.firstOrNull { param -> param.required }?.let { param ->
                    error("Param ${param.name} is required for $pattern path")
                }
                ""
            } else {
                val paramName = it.groups[NAME_GROUP]!!.value
                paramValues[paramName] ?: if (paramsFromSchema[paramName]!!.required) error("Message must contain $paramName param as required for $pattern path") else ""
            }.urlEncode()
        }
    }

    companion object {
        private fun String.urlEncode() = URLEncoder.encode(urlDecode(), Charsets.UTF_8)
        private fun String.urlDecode() = URLDecoder.decode(this, Charsets.UTF_8)
        private const val PARAM_SEPARATOR = '&'
        private const val NAME_VALUE_SEPARATOR = '='
        private const val ANY_VALUE_PATTERN = "[^\\/&?]+"
        private const val PARAM_PLACEHOLDER = "__PARAM__"
        private const val NAME_GROUP = "name"
        private val PARAM_MATCHER = """\{(?<$NAME_GROUP>\w+)\}""".toRegex()
    }
}