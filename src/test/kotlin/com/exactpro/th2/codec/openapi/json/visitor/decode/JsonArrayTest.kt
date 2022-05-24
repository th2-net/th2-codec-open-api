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

package com.exactpro.th2.codec.openapi.json.visitor.decode

import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonArrayVisitor
import com.exactpro.th2.common.assertInt
import com.exactpro.th2.common.assertList
import com.exactpro.th2.common.assertMessage
import com.exactpro.th2.common.assertString
import com.exactpro.th2.common.message.get
import com.exactpro.th2.common.value.getList
import com.exactpro.th2.common.value.toValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.exactpro.th2.codec.openapi.utils.createArrayTestSchema
import com.exactpro.th2.codec.openapi.utils.getResourceAsText
import com.exactpro.th2.codec.openapi.writer.visitors.VisitorSettings
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat

class JsonArrayTest {

    @Suppress("CAST_NEVER_SUCCEEDS")
    @Test
    fun `not supported decode`() {
        val node = mapper.createArrayNode()
        val visitor = DecodeJsonArrayVisitor(node, VisitorSettings(openAPI, SimpleDateFormat()))
        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            visitor.visit("", StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            visitor.visit("", IntegerSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            visitor.visit("", NumberSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            visitor.visit("", BooleanSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            visitor.visit("", ObjectSchema(), true)
        }
    }

    @Test
    fun `object array test decode`() {
        val fieldName = "objectField"

        val stringName = "stringField"
        val stringValue = "stringValue"

        val integerName = "integerField"
        val integerValue = 123

        val booleanName = "booleanField"
        val booleanValue = false

        val floatName = "floatField"
        val floatValue = 321.123f

        val includedObject = "includedObjectField"
        val includedObjectValue = mapper.createObjectNode().apply {
            this.put(stringName, stringValue)
        }


        val jsonArrayNode = mapper.createArrayNode().apply {
            add(mapper.createObjectNode().apply {
                this.put(stringName, stringValue)
            })
            add(mapper.createObjectNode().apply {
                this.put(integerName, integerValue)
            })
            add(mapper.createObjectNode().apply {
                this.put(booleanName, booleanValue)
            })
            add(mapper.createObjectNode().apply {
                this.put(floatName, floatValue)
            })
            add(mapper.createObjectNode().apply {
                this.put(stringName, stringValue)
                this.put(integerName, integerValue)
                this.put(booleanName, booleanValue)
                this.put(floatName, floatValue)
                this.set<ObjectNode>(includedObject, includedObjectValue)
            })
        }

        val result = DecodeJsonArrayVisitor(jsonArrayNode, VisitorSettings(openAPI, SimpleDateFormat())).apply {
            visit(fieldName, openAPI.components.schemas["ArrayObjectTest"]!! as ArraySchema, true)
        }.getResult()

        (result[fieldName]!!).let { listValue ->
            val list = listValue.getList()!!
            list[0].messageValue.assertString(stringName, stringValue)
            list[1].messageValue.assertInt(integerName, integerValue)
            list[2].messageValue.assertString(booleanName, booleanValue.toString())
            list[3].messageValue.assertString(floatName, floatValue.toString())
            list[4].messageValue.let { bigMessage ->
                bigMessage.assertString(stringName, stringValue)
                bigMessage.assertInt(integerName, integerValue)
                bigMessage.assertString(booleanName, booleanValue.toString())
                bigMessage.assertString(floatName, floatValue.toString())
                bigMessage.assertMessage(includedObject).assertString(stringName, stringValue)
            }
        }
    }

    @Test
    fun `string array test decode`() {
        val fieldName = "stringField"
        val collection = listOf("stringValue1", "stringValue2", "stringValue3", "stringValue4")
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode, VisitorSettings(openAPI, SimpleDateFormat()))
        visitor.visit(fieldName, createArrayTestSchema("string"), true)
        visitor.getResult().build().assertList(fieldName, collection.map {it.toValue()})
    }

    @Test
    fun `boolean array test decode`() {
        val fieldName = "booleanField"
        val collection = listOf(true, false, false, true)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode, VisitorSettings(openAPI, SimpleDateFormat()))
        visitor.visit(fieldName, createArrayTestSchema("boolean"), true)
        visitor.getResult().build().assertList(fieldName, collection.map {it.toValue()})
    }

    @Test
    fun `integer array test decode`() {
        val fieldName = "intField"
        val collection = listOf(1, 2, 2, 4)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode, VisitorSettings(openAPI, SimpleDateFormat()))
        visitor.visit(fieldName, createArrayTestSchema("integer"), true)
        visitor.getResult().build().assertList(fieldName, collection.map {it.toValue()})
    }

    @Test
    fun `double array test decode`() {
        val fieldName = "doubleField"
        val collection = listOf(1.1, 2.2, 2.1, 4.3)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode, VisitorSettings(openAPI, SimpleDateFormat()))
        visitor.visit(fieldName, createArrayTestSchema("number", "double"), true)
        visitor.getResult().build().assertList(fieldName, collection.map {it.toValue()})
    }

    @Test
    fun `float array test decode`() {
        val fieldName = "floatField"
        val collection = listOf(1.1f, 2.2f, 2.1f, 4.3f)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode, VisitorSettings(openAPI, SimpleDateFormat()))
        visitor.visit(fieldName, createArrayTestSchema("number", "float"), true)
        visitor.getResult().build().assertList(fieldName, collection.map {it.toValue()})
    }

    @Test
    fun `long array test decode`() {
        val fieldName = "longField"
        val collection = listOf(1111121111L, 2222222222L, 2111331111L, 433333223333L)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode, VisitorSettings(openAPI, SimpleDateFormat()))
        visitor.visit(fieldName, createArrayTestSchema("integer", "int64"), true)
        visitor.getResult().build().assertList(fieldName, collection.map {it.toValue()})
    }

    @Test
    fun `big decimal array test decode`() {
        val fieldName = "decimalField"
        val collection = listOf(100000000000, 100003330000, 100000220000, 100000001100)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode, VisitorSettings(openAPI, SimpleDateFormat()))
        visitor.visit(fieldName, createArrayTestSchema("integer", "int64"), true)
        visitor.getResult().build().assertList(fieldName, collection.map {it.toValue()})
    }

    private companion object {
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/visitorTests.yml"), null, OpenApiCodecSettings().dictionaryParseOption).openAPI
        private val mapper = ObjectMapper()
    }
}