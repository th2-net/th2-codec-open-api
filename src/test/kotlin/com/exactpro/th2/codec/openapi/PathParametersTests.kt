package com.exactpro.th2.codec.openapi

import com.exactpro.th2.common.assertString
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PathParametersTests {
    @Test
    fun `simple params test encode request`() {
        val testPath = "123"
        val header = message("TestPathGet").apply {
            addField("UriParameters", message().apply {
                addField("path", testPath)
            })
        }.build()
        val result = OpenApiCodec(openAPI).encode(MessageGroup.newBuilder().addMessages(AnyMessage.newBuilder().setMessage(header).build()).build())

        Assertions.assertEquals(1, result.messagesList.size)
        Assertions.assertTrue(result.messagesList[0].hasMessage())
        val headerResult = result.messagesList[0].message!!

        headerResult.assertString(OpenApiCodec.URI_FIELD, "/test/123")
        headerResult.assertString(OpenApiCodec.METHOD_FIELD, "get")
    }

    @Test
    fun `empty required params test encode `() {
        val codec = OpenApiCodec(openAPI)
        Assertions.assertThrows(IllegalStateException::class.java) {
            codec.encode(message("TestPathGet").apply {
                addField("UriParameters", message())
            }.build())
        }
        Assertions.assertThrows(IllegalStateException::class.java) {
            codec.encode(message("TestPathGet").build())
        }
    }

    @Test
    fun `empty non required params test encode `() {
        val codec = OpenApiCodec(openAPI)
        Assertions.assertDoesNotThrow  {
            val result = codec.encode(message("EmptyPathPathGet").apply {
                addField("UriParameters", message())
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

    companion object {
        private val settings = OpenApiCodecSettings()
        private val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/params-tests.yml"), null, settings.dictionaryParseOption).openAPI
    }
}