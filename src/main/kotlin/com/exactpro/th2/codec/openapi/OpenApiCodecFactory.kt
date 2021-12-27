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

package com.exactpro.th2.codec.openapi

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.api.IPipelineCodecContext
import com.exactpro.th2.codec.api.IPipelineCodecFactory
import com.exactpro.th2.codec.api.IPipelineCodecSettings
import com.exactpro.th2.common.schema.dictionary.DictionaryType
import com.google.gson.Gson
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
        LOGGER.debug { "Context settled" }
    }

    override fun create(settings: IPipelineCodecSettings?): IPipelineCodec {
        check(this::context.isInitialized) { "Open api context was not loaded" }
        val openApiSettings = requireNotNull(settings as? OpenApiCodecSettings) {
            "settings is not an instance of ${OpenApiCodecSettings::class.java}: $settings"
        }
        val content = context[DictionaryType.MAIN].readAllBytes().decodeToString()
        val result = OpenAPIParser().readContents(content, null, openApiSettings.dictionaryParseOption)

        LOGGER.info { "Starting validation with settings: ${Gson().toJson(openApiSettings)}" }

        result.validate(openApiSettings.validationSettings)

        LOGGER.info { "${result.openAPI.info.title} OpenApi dictionary was loaded and validated" }

        return OpenApiCodec(result.openAPI)
    }

    private fun SwaggerParseResult.validate(validationSettings: RuleConfiguration) {
        DictionaryValidator(validationSettings).validate(this)
    }

    companion object {
        const val PROTOCOL = "openapi"
        private val LOGGER = KotlinLogging.logger { }
    }

}