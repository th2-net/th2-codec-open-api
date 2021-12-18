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
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.plusAssign
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import getResourceAsText
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JsonArrayEncodeTests {

    @Test
    fun `simple test array json encode response`() {
        val codec = OpenApiCodec(openAPI, settings)
        val jsonMessage = message("StoreGet200ApplicationJson").apply {
            metadataBuilder.protocol = "openapi"
            addField("array", listOf("test0", "test1", "test2"))
        }.build()

        val messageGroup = MessageGroup.newBuilder()
        messageGroup += jsonMessage

        val result = codec.encode(messageGroup.build())

        Assertions.assertEquals(1, result.messagesList.size)
        Assertions.assertTrue(result.messagesList[0].hasRawMessage())
        val jsonString = result.messagesList[0].rawMessage.body.toStringUtf8()

        mapper.readTree(jsonString).let { json ->
            Assertions.assertTrue(json.isArray) { "Result of encode must be array" }
            val arrayJson = (json as ArrayNode)
            Assertions.assertEquals(3, arrayJson.count())
            for (i in 0..2) {
                Assertions.assertEquals("test$i", arrayJson.get(i).asText())
            }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private val settings = OpenApiCodecSettings()
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/valid-dictionary.yml"), null, settings.dictionaryParseOption).openAPI
        private val mapper = ObjectMapper()
    }

}