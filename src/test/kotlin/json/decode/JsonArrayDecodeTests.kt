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

package json.decode

import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.codec.openapi.writer.SchemaWriter.Companion.ARRAY_TYPE
import com.exactpro.th2.common.assertList
import com.exactpro.th2.common.value.toValue
import getResourceAsText
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import testDecode

class JsonArrayDecodeTests {

    @Test
    fun `simple test json array decode response`() {
        val jsonData = """["test1", "test2", "test3"]"""
        val decodedResult = OpenApiCodec(openAPI, settings).testDecode(
            "/test",
            "get",
            "200",
            "application/json",
            jsonData)
        decodedResult!!.assertList(ARRAY_TYPE, listOf("test1".toValue(), "test2".toValue(), "test3".toValue()))
    }

    @Test
    fun `simple test json array decode request`() {
        val jsonData = """["test1", "test2", "test3"]"""
        val decodedResult = OpenApiCodec(openAPI, settings).testDecode(
            "/test",
            "get",
            null,
            "application/json",
            jsonData)
        decodedResult!!.assertList(ARRAY_TYPE, listOf("test1".toValue(), "test2".toValue(), "test3".toValue()))
    }

    @Test
    fun `test json array decode request without body`() {
        val decodedResult = OpenApiCodec(openAPI, settings).testDecode(
            "/test",
            "get",
            null,
            null)
        Assertions.assertNull(decodedResult)
    }

    @Test
    fun `test json array decode response without body`() {
        val decodedResult = OpenApiCodec(openAPI, settings).testDecode(
            "/test",
            "get",
            "200",
            null)
        Assertions.assertNull(decodedResult)
    }

    companion object {
        private val settings = OpenApiCodecSettings()
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/array-json-tests.yml"), null, settings.dictionaryParseOption).openAPI
    }
}