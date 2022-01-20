package com.exactpro.th2.codec.openapi

import com.exactpro.th2.codec.openapi.OpenApiCodec.Companion.HEADERS_FIELD
import com.exactpro.th2.codec.openapi.OpenApiCodec.Companion.HEADER_PARAMS_FIELD
import com.exactpro.th2.codec.openapi.OpenApiCodec.Companion.URI_PARAMS_FIELD
import com.exactpro.th2.common.assertList
import com.exactpro.th2.common.assertString
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.sessionAlias
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ParametersTests {

    @Test
    fun `simple params test encode request`() {
        val testPath = "123"
        val header = message("TestPathGet").apply {
            addField("uriParameters", message().apply {
                addField("path", testPath)
            })
        }.build()
        val result = codec.encode(MessageGroup.newBuilder().addMessages(AnyMessage.newBuilder().setMessage(header).build()).build())

        Assertions.assertEquals(1, result.messagesList.size)
        Assertions.assertTrue(result.messagesList[0].hasMessage())
        val headerResult = result.messagesList[0].message!!

        headerResult.assertString(OpenApiCodec.URI_FIELD, "/test/123")
        headerResult.assertString(OpenApiCodec.METHOD_FIELD, "get")
    }

    @Test
    fun `empty required params test encode `() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            codec.encode(message("TestPathGet").apply {
                addField("uriParameters", message())
            }.build())
        }
        Assertions.assertThrows(IllegalStateException::class.java) {
            codec.encode(message("TestPathGet").build())
        }
    }

    @Test
    fun `empty non required params test encode `() {
        Assertions.assertDoesNotThrow  {
            val result = codec.encode(message("EmptyPathPathGet").apply {
                addField("uriParameters", message())
            }.build())

            Assertions.assertEquals(1, result.messagesList.size)
            Assertions.assertTrue(result.messagesList[0].hasMessage())
            val headerResult = result.messagesList[0].message!!

            headerResult.assertString(OpenApiCodec.URI_FIELD, "/empty?path=")
            headerResult.assertString(OpenApiCodec.METHOD_FIELD, "get")
        }

        Assertions.assertDoesNotThrow {
            val result = codec.encode(message("EmptyPathPathGet").build())

            Assertions.assertEquals(1, result.messagesList.size)
            Assertions.assertTrue(result.messagesList[0].hasMessage())
            val headerResult = result.messagesList[0].message!!

            headerResult.assertString(OpenApiCodec.URI_FIELD, "/empty?path=")
            headerResult.assertString(OpenApiCodec.METHOD_FIELD, "get")
        }
    }

    @Test
    fun `headers test encode `() {
        val headers = mapOf("some-header" to "test")

        val result = codec.encode(message("TestLabelActivatePost").apply {
            addField(URI_PARAMS_FIELD, message().apply {
                addField("label", "testLabel")
            })
            headers.forEach {
                addField(HEADER_PARAMS_FIELD, message().apply {
                    addField(it.key, it.value)
                })
            }
        }.build())

        Assertions.assertEquals(1, result.messagesList.size)
        Assertions.assertTrue(result.messagesList[0].hasMessage())
        val headerResult = result.messagesList[0].message!!

        headerResult.assertString(OpenApiCodec.URI_FIELD, "/test/testLabel/activate")
        headerResult.assertString(OpenApiCodec.METHOD_FIELD, "post")

        val resultHeaders = headerResult.assertList(HEADERS_FIELD)

        headers.forEach { header ->
            Assertions.assertNotNull(resultHeaders.firstOrNull { it.messageValue.getString("name") == header.key && it.messageValue.getString("value") == header.value }) {
                "Message must contain header ${header.key} with value ${header.value}"
            }
        }
    }

    @Test
    fun `empty headers while required test encode `() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            codec.encode(message("TestLabelActivatePost").apply {
                addField(URI_PARAMS_FIELD, message().apply {
                    addField("label", "testLabel")
                })
            }.build())
        }
    }

    private companion object {
        val settings = OpenApiCodecSettings()
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/params-tests.yml"), null, settings.dictionaryParseOption).openAPI
        val codec = OpenApiCodec(openAPI, settings)
    }
}