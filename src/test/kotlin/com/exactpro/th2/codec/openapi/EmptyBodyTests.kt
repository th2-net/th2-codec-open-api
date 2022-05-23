/*
 * Copyright 2021-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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