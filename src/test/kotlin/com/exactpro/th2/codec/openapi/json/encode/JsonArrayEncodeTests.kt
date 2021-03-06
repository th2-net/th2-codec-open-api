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

package com.exactpro.th2.codec.openapi.json.encode

import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.common.message.addField
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.exactpro.th2.codec.openapi.utils.getResourceAsText
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import com.exactpro.th2.codec.openapi.utils.testEncode

class JsonArrayEncodeTests {

    @Test
    fun `simple test array json encode response`() {
        val rawMessage = OpenApiCodec(openAPI, settings).testEncode(
            "/test",
            "get",
            "200",
            "application/json",
            "json") {
            addField("array", listOf("test0", "test1", "test2"))
        }
        val jsonString = rawMessage!!.body.toStringUtf8()

        mapper.readTree(jsonString).let { json ->
            Assertions.assertTrue(json.isArray) { "Result of encode must be array" }
            val arrayJson = (json as ArrayNode)
            Assertions.assertEquals(3, arrayJson.count())
            for (i in 0..2) {
                Assertions.assertEquals("test$i", arrayJson.get(i).asText())
            }
        }
    }

    @Test
    fun `simple test array json encode request`() {
        val rawMessage = OpenApiCodec(openAPI, settings).testEncode(
            "/test",
            "get",
            null,
            "application/json",
            "json") {
            addField("array", listOf("test0", "test1", "test2"))
        }
        val jsonString = rawMessage!!.body.toStringUtf8()

        mapper.readTree(jsonString).let { json ->
            Assertions.assertTrue(json.isArray) { "Result of encode must be array" }
            val arrayJson = (json as ArrayNode)
            Assertions.assertEquals(3, arrayJson.count())
            for (i in 0..2) {
                Assertions.assertEquals("test$i", arrayJson.get(i).asText())
            }
        }
    }

    private companion object {
        val settings = OpenApiCodecSettings()
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/array-json-tests.yml"), null, settings.dictionaryParseOption).openAPI
        val mapper = ObjectMapper()
    }

}