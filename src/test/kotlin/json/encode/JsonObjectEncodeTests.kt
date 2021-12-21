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

package json.encode

import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.common.message.addField
import com.fasterxml.jackson.databind.ObjectMapper
import getResourceAsText
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import testEncode

class JsonObjectEncodeTests {

    @Test
    fun `json encode response`() {
        val rawMessage = OpenApiCodec(openAPI, settings).testEncode("/test/object", "get", "200", "application/json") {
            addField("publicKey", "1234567")
            addField("testEnabled", true)
            addField("testStatus", "FAILED")
        }
        val jsonString = rawMessage.body.toStringUtf8()

        Assertions.assertEquals("200", rawMessage.metadata.propertiesMap[OpenApiCodec.CODE_PROPERTY])
        Assertions.assertEquals("get", rawMessage.metadata.propertiesMap[OpenApiCodec.METHOD_PROPERTY])
        Assertions.assertEquals("/test/object", rawMessage.metadata.propertiesMap[OpenApiCodec.URI_PROPERTY])

        LOGGER.info { "JSON: \n$jsonString" }

        mapper.readTree(jsonString).let { json ->
            Assertions.assertEquals(3, json.size())
            Assertions.assertEquals("1234567", json.get("publicKey").asText())
            Assertions.assertTrue(json.get("testEnabled").asBoolean())
            Assertions.assertEquals("FAILED", json.get("testStatus").asText())
        }
    }

    @Test
    fun `json encode request`() {
        val rawMessage = OpenApiCodec(openAPI, settings).testEncode("/test", "get", null, "application/json") {
            addField("publicKey", "1234567")
            addField("testEnabled", true)
            addField("testStatus", "FAILED")
        }

        val jsonString = rawMessage.body.toStringUtf8()

        Assertions.assertEquals("get", rawMessage.metadata.propertiesMap[OpenApiCodec.METHOD_PROPERTY])
        Assertions.assertEquals("/test", rawMessage.metadata.propertiesMap[OpenApiCodec.URI_PROPERTY])

        LOGGER.info { "JSON: \n$jsonString" }

        mapper.readTree(jsonString).let { json ->
            Assertions.assertEquals(3, json.size())
            Assertions.assertEquals("1234567", json.get("publicKey").asText())
            Assertions.assertTrue(json.get("testEnabled").asBoolean())
            Assertions.assertEquals("FAILED", json.get("testStatus").asText())
        }
    }

    private companion object {
        val LOGGER = KotlinLogging.logger { }
        val settings = OpenApiCodecSettings()
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/valid-dictionary.yml"), null, settings.dictionaryParseOption).openAPI
        val mapper = ObjectMapper()
    }
}