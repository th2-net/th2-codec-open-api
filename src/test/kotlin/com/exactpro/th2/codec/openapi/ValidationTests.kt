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

import com.exactpro.th2.codec.openapi.throwable.DictionaryValidationException
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.openapitools.codegen.validations.oas.RuleConfiguration
import java.io.File
import java.net.URI

class ValidationTests {

    @Test
    fun `valid dictionary`() {
        Assertions.assertDoesNotThrow {
            File(ValidationTests::class.java.classLoader.getResource("dictionaries/valid/")!!.path).walk().forEach { dictionary ->
                if (dictionary.name.endsWith(".yml")) {
                    validator.validate(parser.readLocation(dictionary.path, null, parseOptions))
                    LOGGER.info { "Validated valid dictionary ${dictionary.name} from test method" }
                }
            }
        }
    }

    @Test
    fun `valid dictionary in factory`() {
        Assertions.assertDoesNotThrow {
            File(ValidationTests::class.java.classLoader.getResource("dictionaries/valid/")!!.path).walk().forEach { dictionary ->
                if (dictionary.name.endsWith(".yml")) {
                    val factory = OpenApiCodecFactory().apply {
                        init { dictionary.inputStream() }
                    }

                    factory.create(OpenApiCodecSettings().apply {
                        dictionaryParseOption = parseOptions
                        validationSettings = ObjectMapper().readValue(getJsonConfiguration().toURL(), RuleConfiguration::class.java)
                    })
                    LOGGER.info { "Validated valid dictionary ${dictionary.name} inside of factory" }
                }
            }
        }
    }

    @Test
    fun `invalid dictionary`() {
        File(ValidationTests::class.java.classLoader.getResource("dictionaries/invalid/")!!.path).walk().forEach { dictionary ->
            if (dictionary.name.endsWith(".yml")) {
                Assertions.assertThrows(DictionaryValidationException::class.java) {
                    validator.validate(parser.readLocation(dictionary.path, null, parseOptions))
                }

                LOGGER.info { "Validated invalid dictionary ${dictionary.name} from test method" }
            }
        }
    }

    @Test
    fun `invalid dictionary in factory`() {
        File(ValidationTests::class.java.classLoader.getResource("dictionaries/invalid/")!!.path).walk().forEach { dictionary ->
            if (dictionary.name.endsWith(".yml")) {
                val factory = OpenApiCodecFactory().apply {
                    init { dictionary.inputStream() }
                }
                Assertions.assertThrows(DictionaryValidationException::class.java) {
                    factory.create(OpenApiCodecSettings().apply {
                        dictionaryParseOption = parseOptions
                        validationSettings =
                            ObjectMapper().readValue(getJsonConfiguration().toURL(), RuleConfiguration::class.java)
                    })
                }
                LOGGER.info { "Validated invalid dictionary ${dictionary.name} from factory" }
            }
        }
    }

    private companion object {
        val LOGGER = KotlinLogging.logger { }
        val parser = OpenAPIParser()
        val parseOptions = ParseOptions().apply { isResolve = true }
        val validator = DictionaryValidator(ObjectMapper().readValue(getJsonConfiguration().toURL(), RuleConfiguration::class.java))
        private fun getJsonConfiguration(): URI {
            return requireNotNull(this::class.java.classLoader.getResource("rule-config.json")) {"Rule configuration from resources required"}.toURI()
        }
    }
}