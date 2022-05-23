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
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonObjectVisitor
import com.exactpro.th2.common.assertDouble
import com.exactpro.th2.common.assertInt
import com.exactpro.th2.common.assertList
import com.exactpro.th2.common.assertMessage
import com.exactpro.th2.common.assertString
import com.exactpro.th2.common.message.get
import com.exactpro.th2.common.value.getList
import com.exactpro.th2.common.value.toValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.exactpro.th2.codec.openapi.createArrayTestSchema
import com.exactpro.th2.codec.openapi.createTestSchema
import com.exactpro.th2.codec.openapi.utils.getResourceAsText
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import org.junit.jupiter.api.Test

@Suppress("CAST_NEVER_SUCCEEDS")
class JsonObjectTest {

    @Test
    fun `object test decode`() {
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

        val objectValue = mapper.createObjectNode().apply {
            this.put(stringName, stringValue)
            this.put(integerName, integerValue)
            this.put(booleanName, booleanValue)
            this.put(floatName, floatValue)
            this.set<ObjectNode>(includedObject, includedObjectValue)
        }
        val json = mapper.createObjectNode().apply {
            this.set<ObjectNode>(fieldName, objectValue)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, openAPI.components.schemas["ObjectTest"]!! as ObjectSchema, true)
        }.getResult()
        result[fieldName]!!.messageValue.let { bigMessage ->
            bigMessage.assertString(stringName, stringValue)
            bigMessage.assertInt(integerName, integerValue)
            bigMessage.assertString(booleanName, booleanValue.toString())
            bigMessage.assertString(floatName, floatValue.toString())
            bigMessage.assertMessage(includedObject).assertString(stringName, stringValue)
        }

    }

    @Test
    fun `string test decode`() {
        val fieldName = "stringField"
        val simpleValue = "stringValue"
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createTestSchema(simpleValue) as StringSchema, true)
        }.getResult()
        result.build().assertString(fieldName, simpleValue)
    }

    @Test
    fun `boolean test decode`() {
        val fieldName = "booleanField"
        val simpleValue = false
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createTestSchema(simpleValue) as BooleanSchema, true)
        }.getResult()
        result.build().assertString(fieldName, simpleValue.toString())
    }

    @Test
    fun `int test decode`() {
        val fieldName = "intField"
        val simpleValue = 12345
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createTestSchema(simpleValue) as IntegerSchema, true)
        }.getResult()
        result.build().assertInt(fieldName, simpleValue)
    }

    @Test
    fun `double test decode`() {
        val fieldName = "doubleField"
        val simpleValue = 12345.67
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createTestSchema(simpleValue) as NumberSchema, true)
        }.getResult()
        result.build().assertDouble(fieldName, simpleValue)
    }

    @Test
    fun `float test decode`() {
        val fieldName = "floatField"
        val simpleValue = 12345.12f
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createTestSchema(simpleValue) as NumberSchema, true)
        }.getResult()
        result.build().assertString(fieldName, simpleValue.toString())
    }

    @Test
    fun `long test decode`() {
        val fieldName = "longField"
        val simpleValue = 1234512345L
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createTestSchema(simpleValue) as IntegerSchema, true)
        }.getResult()
        result.build().assertString(fieldName, simpleValue.toString())
    }

    @Test
    fun `big decimal test decode`() {
        val fieldName = "decimalField"
        val simpleValue = 100000000000
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createTestSchema(simpleValue) as IntegerSchema, true)
        }.getResult()
        result.build().assertString(fieldName, simpleValue.toString())
    }

    @Test
    fun `string array test decode`() {
        val fieldName = "stringArrayField"
        val collection = listOf("stringValue1", "stringValue3", "stringValue2", "stringValue4")
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createArrayTestSchema("string"), true)
        }.getResult()
        result.build().assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `boolean array test decode`() {
        val fieldName = "booleanArrayField"
        val collection = listOf(true, false, false, true)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createArrayTestSchema("string"), true)
        }.getResult()
        result.build().assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `int array test decode`() {
        val fieldName = "intArrayField"
        val collection = listOf(1, 3, 2, 4)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createArrayTestSchema("integer"), true)
        }.getResult()
        result.build().assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `double array test decode`() {
        val fieldName = "doubleArrayField"
        val collection = listOf(0.1, 0.1, 0.3, 0.2)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createArrayTestSchema("string"), true)
        }.getResult()
        result.build().assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `float array test decode`() {
        val fieldName = "floatArrayField"
        val collection = listOf(0.1f, 0.1f, 0.3f, 0.2f)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createArrayTestSchema("string"), true)
        }.getResult()
        result.build().assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `long array test decode`() {
        val fieldName = "longArrayField"
        val collection = listOf(123123123L, 321312321L, 333333333L, 444444444L)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createArrayTestSchema("string"), true)
        }.getResult()
        result.build().assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `big decimal array test decode`() {
        val fieldName = "decimalArrayField"
        val collection = listOf(100000400000, 100003000000, 100000001000, 100000020000)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
            visit(fieldName, createArrayTestSchema("string"), true)
        }.getResult()
        result.build().assertList(fieldName, collection.map { it.toValue() })
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

        val json =  mapper.createObjectNode().apply {
            this.set<ObjectNode>(fieldName, jsonArrayNode)
        }

        val result = DecodeJsonObjectVisitor(json, openAPI).apply {
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


    private companion object {
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/visitorTests.yml"), null, OpenApiCodecSettings().dictionaryParseOption).openAPI
        private val mapper = ObjectMapper()
    }
}