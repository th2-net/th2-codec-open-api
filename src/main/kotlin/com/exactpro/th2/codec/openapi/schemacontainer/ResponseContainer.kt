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

package com.exactpro.th2.codec.openapi.schemacontainer

import com.exactpro.th2.codec.openapi.UriPattern
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter

class ResponseContainer(
    uriPattern: UriPattern,
    method: String,
    val code: String,
    body: Schema<*>?,
    bodyFormat: String?,
    params: Map<String, Parameter>
) : HttpRouteContainer(uriPattern, method, body, bodyFormat, params) {

}