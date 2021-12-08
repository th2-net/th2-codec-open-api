import com.exactpro.th2.codec.api.IPipelineCodecContext
import com.exactpro.th2.codec.openapi.OpenApiCodecFactory
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.codec.openapi.dictionary.DictionaryException
import com.exactpro.th2.codec.openapi.dictionary.OpenApiValidator
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.openapitools.codegen.validations.oas.RuleConfiguration
import java.io.File
import java.io.InputStream
import java.net.URI

class ValidationTests {

    @Test
    fun `valid dictionary`() {
        val dictionary = requireNotNull(ValidationTests::class.java.getResource("valid-dictionary.yml")) {"Dictionary from resources required"}.toURI().path.drop(1)
        val result = OpenAPIParser().readLocation(dictionary, null, parseOptions)

        validator.validate(result)
    }

    @Test
    fun `valid dictionary in factory`() {
        val dictionary = requireNotNull(ValidationTests::class.java.getResource("valid-dictionary.yml")) {"Dictionary from resources required"}.toURI().path.drop(1)

        val factory = OpenApiCodecFactory().apply {
            init { File(dictionary).inputStream() }
        }

        factory.create(OpenApiCodecSettings().apply {
            dictionaryParseOption = parseOptions
            validationSettings = ObjectMapper().readValue(getJsonConfiguration().toURL(), RuleConfiguration::class.java)
        })
    }

    @Test
    fun `invalid dictionary`() {
        val dictionary = requireNotNull(ValidationTests::class.java.getResource("invalid-dictionary.yml")) {"Dictionary from resources required"}.toURI().path.drop(1)
        val result = OpenAPIParser().readLocation(dictionary, null, parseOptions)

        Assertions.assertThrows(DictionaryException::class.java) {
            validator.validate(result)
        }
    }

    @Test
    fun `invalid dictionary in factory`() {
        val dictionary = requireNotNull(ValidationTests::class.java.getResource("invalid-dictionary.yml")) {"Dictionary from resources required"}.toURI().path.drop(1)
        val factory = OpenApiCodecFactory().apply {
            init { File(dictionary).inputStream() }
        }
        Assertions.assertThrows(DictionaryException::class.java) {
            factory.create(OpenApiCodecSettings().apply {
                dictionaryParseOption = parseOptions
                validationSettings =
                    ObjectMapper().readValue(getJsonConfiguration().toURL(), RuleConfiguration::class.java)
            })
        }
    }

    companion object {
        val parseOptions = ParseOptions().apply { isResolve = true }
        val validator = OpenApiValidator(ObjectMapper().readValue(getJsonConfiguration().toURL(), RuleConfiguration::class.java))
        private fun getJsonConfiguration(): URI {
            return requireNotNull(ValidationTests::class.java.getResource("rule-config.json")) {"Rule configuration from resources required"}.toURI()
        }
    }
}