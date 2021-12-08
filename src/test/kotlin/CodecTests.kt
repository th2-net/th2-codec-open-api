import com.exactpro.th2.codec.openapi.OpenApiCodec
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.plusAssign
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.parser.OpenAPIParser
import mu.KotlinLogging
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
            Assertions.assertEquals("1234567", json.get("publicKey").asText())
            Assertions.assertTrue(json.get("testEnabled").asBoolean())
            Assertions.assertEquals("FAILED", json.get("testStatus").asText())
        }
    }

    companion object {
        val settings = OpenApiCodecSettings()
        val dictionary = requireNotNull(CodecTests::class.java.getResource("valid-dictionary.yml")) {"Dictionary from resources required"}.toURI().path.drop(1)
        val openAPI = OpenAPIParser().readLocation(dictionary, null, settings.dictionaryParseOption).openAPI
        val mapper = ObjectMapper()
    }
}