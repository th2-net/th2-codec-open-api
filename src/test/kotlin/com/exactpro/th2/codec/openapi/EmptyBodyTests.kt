package com.exactpro.th2.codec.openapi

import com.exactpro.th2.codec.openapi.utils.getResourceAsText
import com.exactpro.th2.codec.openapi.utils.testDecode
import com.exactpro.th2.codec.openapi.utils.testEncode
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EmptyBodyTests {
    @Test
    fun `simple request test encode`() {
        val rawMessage = OpenApiCodec(openAPI, settings).testEncode(
            "/test",
            "get",
            null,
            null,
            "")
        Assertions.assertNull(rawMessage)
    }

    @Test
    fun `simple request test decode`() {
        val rawMessage = OpenApiCodec(openAPI, settings).testDecode(
            "/test",
            "get",
            null,
            null)
        Assertions.assertNull(rawMessage)
    }

    @Test
    fun `simple response test encode`() {
        val rawMessage = OpenApiCodec(openAPI, settings).testEncode(
            "/test",
            "get",
            "200",
            null,
            "")
        Assertions.assertNull(rawMessage)
    }

    @Test
    fun `simple response test decode`() {
        val rawMessage = OpenApiCodec(openAPI, settings).testDecode(
            "/test",
            "get",
            "200",
            null)
        Assertions.assertNull(rawMessage)
    }

    @Test
    fun `test json array decode request without body`() {
        val decodedResult = OpenApiCodec(openAPI, settings).testDecode(
            "/testBody",
            "get",
            null,
            null)
        Assertions.assertNull(decodedResult)
    }

    @Test
    fun `test json array decode response without body`() {
        val decodedResult = OpenApiCodec(openAPI, settings).testDecode(
            "/testBody",
            "get",
            "200",
            null)
        Assertions.assertNull(decodedResult)
    }

    companion object {
        private val settings = OpenApiCodecSettings()
        private val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/empty-body-tests.yml"), null, settings.dictionaryParseOption).openAPI
    }
}