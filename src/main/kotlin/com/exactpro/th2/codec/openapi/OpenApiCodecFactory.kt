package com.exactpro.th2.codec.openapi

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.api.IPipelineCodecContext
import com.exactpro.th2.codec.api.IPipelineCodecFactory
import com.exactpro.th2.codec.api.IPipelineCodecSettings
import com.exactpro.th2.codec.openapi.dictionary.OpenApiValidator
import com.exactpro.th2.common.schema.dictionary.DictionaryType
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.SwaggerParseResult
import mu.KotlinLogging
import org.openapitools.codegen.validations.oas.RuleConfiguration
import java.io.BufferedReader

@Suppress("unused")
class OpenApiCodecFactory : IPipelineCodecFactory {

    override val settingsClass: Class<out IPipelineCodecSettings> = OpenApiCodecSettings::class.java
    override val protocol: String = PROTOCOL
    private lateinit var context: IPipelineCodecContext

    override fun init(pipelineCodecContext: IPipelineCodecContext) {
        context = pipelineCodecContext
    }

    override fun create(settings: IPipelineCodecSettings?): IPipelineCodec {
        check(this::context.isInitialized) {"Open api context was not loaded"}
        val settings = requireNotNull(settings as? OpenApiCodecSettings) {
            "settings is not an instance of ${OpenApiCodecSettings::class.java}: $settings"
        }
        val content = context[DictionaryType.MAIN].bufferedReader().use(BufferedReader::readText)
        val result = OpenAPIParser().readContents(content, null, settings.dictionaryParseOption)
        result.validate(settings.validationSettings)

        LOGGER.info { "${result.openAPI.info.title} OpenApi dictionary was loaded and validated" }

        return OpenApiCodec(result.openAPI, settings)
    }

    private fun SwaggerParseResult.validate(validationSettings: RuleConfiguration) {
        OpenApiValidator(validationSettings).validate(this)
    }

    companion object {
        const val PROTOCOL = "openapi"
        private val LOGGER = KotlinLogging.logger { }
    }

}