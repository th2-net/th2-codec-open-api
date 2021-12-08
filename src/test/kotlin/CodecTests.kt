/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.plusAssign
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.swagger.parser.OpenAPIParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CodecTests {

    @Test
    fun `simple test json encode`() {
        val codec = OpenApiCodec(openAPI, settings)
        val json = message("TestObjectGet200ApplicationJson").apply {
            metadataBuilder.protocol = "openapi"
            addField("publicKey", "1234567")
            addField("testEnabled", true)
            addField("testStatus", "FAILED")
        }.build()

        val messageGroup = MessageGroup.newBuilder()
        messageGroup += json

        val result = codec.encode(messageGroup.build())

        Assertions.assertEquals(1, result.messagesList.size)
        Assertions.assertTrue(result.messagesList[0].hasRawMessage())
        val jsonString = result.messagesList[0].rawMessage.body.toStringUtf8()

        mapper.readTree(jsonString).let { json ->
            Assertions.assertEquals(3, json.size())
            Assertions.assertEquals("1234567", json.get("publicKey").asText())
            Assertions.assertTrue(json.get("testEnabled").asBoolean())
            Assertions.assertEquals("FAILED", json.get("testStatus").asText())
        }
    }

    @Test
    fun `simple test array json encode`() {
        val codec = OpenApiCodec(openAPI, settings)
        val json = message("StoreGet200ApplicationJson").apply {
            metadataBuilder.protocol = "openapi"
            addField("array", listOf("test0", "test1", "test2"))
        }.build()

        val messageGroup = MessageGroup.newBuilder()
        messageGroup += json

        val result = codec.encode(messageGroup.build())

        Assertions.assertEquals(1, result.messagesList.size)
        Assertions.assertTrue(result.messagesList[0].hasRawMessage())
        val jsonString = result.messagesList[0].rawMessage.body.toStringUtf8()

        mapper.readTree(jsonString).let { json ->
            Assertions.assertTrue(json.isArray) {"Result of encode must be array"}
            val arrayJson = (json as ArrayNode)
            Assertions.assertEquals(3, arrayJson.count())
            for (i in 0..2) {
                Assertions.assertEquals("test$i", arrayJson.get(i).asText())
            }
        }
    }

    companion object {
        val settings = OpenApiCodecSettings()
        val dictionary = requireNotNull(CodecTests::class.java.getResource("valid-dictionary.yml")) {"Dictionary from resources required"}.toURI().path.drop(1)
        val openAPI = OpenAPIParser().readLocation(dictionary, null, settings.dictionaryParseOption).openAPI
        val mapper = ObjectMapper()
    }
}