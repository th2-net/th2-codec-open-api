import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.plusAssign
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.swagger.parser.OpenAPIParser
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
        private val dictionary =
            requireNotNull(JsonObjectDecodeTests::class.java.getResource("valid-dictionary.yml")) { "Dictionary from resources required" }.toURI().path.drop(
                1
            )
        private val openAPI = OpenAPIParser().readLocation(dictionary, null, settings.dictionaryParseOption).openAPI
        private val mapper = ObjectMapper()
    }

}