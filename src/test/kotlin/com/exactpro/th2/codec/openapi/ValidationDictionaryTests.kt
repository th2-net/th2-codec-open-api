package com.exactpro.th2.codec.openapi/*
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

import com.exactpro.th2.codec.api.DictionaryAlias
import com.exactpro.th2.codec.api.IPipelineCodecContext
import com.exactpro.th2.codec.openapi.throwable.DictionaryValidationException
import com.exactpro.th2.common.schema.dictionary.DictionaryType
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.parser.OpenAPIParser
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.openapitools.codegen.validations.oas.RuleConfiguration
import java.io.File
import java.io.InputStream
import java.net.URI

class ValidationDictionaryTests {

    @Test
    fun `valid dictionary`() {
        Assertions.assertDoesNotThrow {
            File(ValidationDictionaryTests::class.java.classLoader.getResource("dictionaries/valid/")!!.path).walk().forEach { dictionary ->
                if (dictionary.name.endsWith(YAML_FORMAT) || dictionary.name.endsWith(JSON_FORMAT)) {
                    validator.validate(parser.readLocation(dictionary.path, null, settings.dictionaryParseOption))
                    LOGGER.info { "Validated valid dictionary ${dictionary.name} from test method" }
                }
            }
        }
    }

    @Test
    fun `valid dictionary in factory`() {
        Assertions.assertDoesNotThrow {
            File(ValidationDictionaryTests::class.java.classLoader.getResource("dictionaries/valid/")!!.path).walk().forEach { dictionary ->
                if (dictionary.name.endsWith(YAML_FORMAT) || dictionary.name.endsWith(JSON_FORMAT)) {
                    val factory = OpenApiCodecFactory().apply {
                        init(object : IPipelineCodecContext {
                            override fun get(alias: DictionaryAlias): InputStream = error("")
                            override fun get(type: DictionaryType): InputStream = dictionary.inputStream()
                            override fun getDictionaryAliases(): Set<String> = error("")
                        })
                    }

                    factory.create(settings)
                    LOGGER.info { "Validated valid dictionary ${dictionary.name} inside of factory" }
                }
            }
        }
    }

    @Test
    fun `invalid dictionary`() {
        File(ValidationDictionaryTests::class.java.classLoader.getResource("dictionaries/invalid/")!!.path).walk().forEach { dictionary ->
            if (dictionary.name.endsWith(YAML_FORMAT) || dictionary.name.endsWith(JSON_FORMAT)) {
                Assertions.assertThrows(DictionaryValidationException::class.java) {
                    validator.validate(parser.readLocation(dictionary.path, null, settings.dictionaryParseOption))
                }

                LOGGER.info { "Validated invalid dictionary ${dictionary.name} from test method" }
            }
        }
    }

    @Test
    fun `invalid dictionary in factory`() {
        File(ValidationDictionaryTests::class.java.classLoader.getResource("dictionaries/invalid/")!!.path).walk().forEach { dictionary ->
            if (dictionary.name.endsWith(YAML_FORMAT) || dictionary.name.endsWith(JSON_FORMAT)) {
                val factory = OpenApiCodecFactory().apply {
                    init(object : IPipelineCodecContext {
                        override fun get(alias: DictionaryAlias): InputStream = TODO("Not yet implemented")

                        override fun get(type: DictionaryType): InputStream = dictionary.inputStream()

                        override fun getDictionaryAliases(): Set<String> = TODO("Not yet implemented")
                    })
                }
                Assertions.assertThrows(DictionaryValidationException::class.java) {
                    factory.create(settings)
                }
                LOGGER.info { "Validated invalid dictionary ${dictionary.name} from factory" }
            }
        }
    }

    private companion object {
        const val JSON_FORMAT = ".json"
        const val YAML_FORMAT = ".yml"
        val LOGGER = KotlinLogging.logger { }
        val parser = OpenAPIParser()
        val settings = OpenApiCodecSettings()
        val validator = DictionaryValidator(ObjectMapper().readValue(getJsonConfiguration().toURL(), RuleConfiguration::class.java))
        private fun getJsonConfiguration(): URI {
            return requireNotNull(this::class.java.classLoader.getResource("rule-config.json")) {"Rule configuration from resources required"}.toURI()
        }
    }
}