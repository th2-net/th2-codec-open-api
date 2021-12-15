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
import com.exactpro.th2.codec.openapi.OpenApiCodec.Companion.HEADERS_FIELD
import com.exactpro.th2.codec.openapi.OpenApiCodec.Companion.METHOD_PROPERTY
import com.exactpro.th2.codec.openapi.OpenApiCodec.Companion.STATUS_CODE_FIELD
import com.exactpro.th2.codec.openapi.OpenApiCodec.Companion.URI_PROPERTY
import com.exactpro.th2.common.assertEqualMessages
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.codec.openapi.writer.SchemaWriter.Companion.ARRAY_TYPE
import com.exactpro.th2.common.assertContains
import com.exactpro.th2.common.assertList
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.plusAssign
import com.exactpro.th2.common.value.toValue
import com.google.protobuf.ByteString
import io.swagger.parser.OpenAPIParser
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JsonArrayDecodeTests {

    @Test
    fun `simple test json array decode response`() {
        val codec = OpenApiCodec(openAPI, settings)
        val response = message("Response").apply {
            addField(STATUS_CODE_FIELD, "200")
            addField(HEADERS_FIELD, listOf(message().apply {
                addField("name", "Content-Type")
                addField("value", "application/json")
            }))
        }.build()

        val body = RawMessage.newBuilder().apply {
            metadataBuilder.putProperties(URI_PROPERTY, "/store")
            metadataBuilder.putProperties(METHOD_PROPERTY, "GET")
            body = ByteString.copyFrom("""["test1", "test2", "test3"]""".trimIndent().toByteArray())
        }.build()

        val group = MessageGroup.newBuilder()

        group += response
        group += body

        val decodeResult = codec.decode(group.build())

        Assertions.assertEquals(2, decodeResult.messagesList.size)
        Assertions.assertTrue(decodeResult.messagesList[0].hasMessage())
        Assertions.assertTrue(decodeResult.messagesList[1].hasMessage())
        assertEqualMessages(response, decodeResult.messagesList[0].message)

        decodeResult.messagesList[1].message.run {
            assertList(ARRAY_TYPE, listOf("test1".toValue(), "test2".toValue(), "test3".toValue()))
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        private val settings = OpenApiCodecSettings()
        private val dictionary =
            requireNotNull(JsonObjectDecodeTests::class.java.getResource("valid-dictionary.yml")) { "Dictionary from resources required" }.toURI().path.drop(
                1
            )
        private val openAPI = OpenAPIParser().readLocation(dictionary, null, settings.dictionaryParseOption).openAPI
    }
}