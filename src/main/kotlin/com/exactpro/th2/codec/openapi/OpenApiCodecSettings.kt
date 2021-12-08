package com.exactpro.th2.codec.openapi

import com.exactpro.th2.codec.api.IPipelineCodecSettings
import io.swagger.v3.parser.core.models.ParseOptions
import org.openapitools.codegen.validations.oas.RuleConfiguration

class OpenApiCodecSettings : IPipelineCodecSettings {
    var validationSettings: RuleConfiguration = RuleConfiguration().apply {
        isEnableRecommendations = true
    }
    var dictionaryParseOption: ParseOptions = ParseOptions().apply {
        isResolve = true
    }
}
