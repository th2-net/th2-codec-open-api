package com.exactpro.th2.codec.openapi.dictionary

import com.exactpro.th2.codec.CodecException
import io.swagger.v3.parser.core.models.SwaggerParseResult
import mu.KotlinLogging
import org.openapitools.codegen.validations.oas.OpenApiEvaluator
import org.openapitools.codegen.validations.oas.RuleConfiguration

class OpenApiValidator(val configuration: RuleConfiguration) {

    fun validate(dictionary: SwaggerParseResult) {
        val openAPI = dictionary.openAPI
        val messages = dictionary.messages

        val evaluator = OpenApiEvaluator(configuration)
        val validationResult = evaluator.validate(openAPI)

        if (validationResult.warnings.isNotEmpty()) {
            logger.warn {"\nSpec has issues or recommendations.\nIssues:\n"}

            validationResult.warnings.forEach {
                logger.warn { "\t${it.message}|${it.details}\n" }
            }
        }

        if (messages.isNotEmpty() || validationResult.errors.isNotEmpty()) {

            logger.error {"\nSpec is invalid.\nIssues:\n"}

            messages.forEach {
                logger.error {"\t$it"}
            }

            validationResult.errors.forEach {
                logger.error { "\t${it.message}\n\t${it.details}" }
            }

            throw DictionaryException("Provided dictionary-spec is not valid.", validationResult.errors, messages)
        } else {
            logger.debug { "Provided dictionary-spec is valid" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}