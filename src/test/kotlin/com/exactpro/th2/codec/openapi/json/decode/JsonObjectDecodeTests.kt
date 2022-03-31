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

package com.exactpro.th2.codec.openapi.json.decode

import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.codec.openapi.utils.getResourceAsText
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import com.exactpro.th2.codec.openapi.utils.testDecode
import com.exactpro.th2.common.message.messageType

class JsonObjectDecodeTests {
    
    @Test
    fun `simple test json decode response`() {
        val jsonData = """{
                      "publicKey" : "1234567",
                      "testEnabled" : true,
                      "testStatus" : "FAILED",
                      "testBigDecimal": 100000000000.0,
                      "testDouble": 1.0
                    }""".trimIndent()
        val decodedResult = OpenApiCodec(openAPI, settings).testDecode(
            "/test",
            "get",
            "200",
            "application/json",
            jsonData)!!

        Assertions.assertEquals("TestGet200AnyAny", decodedResult.messageType)
        Assertions.assertEquals("1234567", decodedResult.getString("publicKey"))
        Assertions.assertEquals(true, decodedResult.getString("testEnabled").toBoolean())
        Assertions.assertEquals("FAILED", decodedResult.getString("testStatus"))
        Assertions.assertEquals(null, decodedResult.getString("nullField"))
        Assertions.assertEquals("100000000000", decodedResult.getString("testBigDecimal"))
        Assertions.assertEquals("1", decodedResult.getString("testDouble"))

    }

    @Test
    fun `simple test json decode request`() {
        val jsonData = """{
                      "publicKey" : "1234567",
                      "testEnabled" : true,
                      "testStatus" : "FAILED"
                    }""".trimIndent()
        val decodedResult = OpenApiCodec(openAPI, settings).testDecode(
            "/test",
            "GET",
            null,
            "application/json",
            jsonData)!!

        Assertions.assertEquals("TestGetApplicationJson", decodedResult.messageType)
        Assertions.assertEquals("1234567", decodedResult.getString("publicKey"))
        Assertions.assertEquals(true, decodedResult.getString("testEnabled").toBoolean())
        Assertions.assertEquals("FAILED", decodedResult.getString("testStatus"))
    }

    private companion object {
        val settings = OpenApiCodecSettings()
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/object-json-tests.yml"), null, settings.dictionaryParseOption).openAPI
    }
}