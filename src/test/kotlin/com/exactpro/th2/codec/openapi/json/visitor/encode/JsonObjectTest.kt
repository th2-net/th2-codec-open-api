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

package com.exactpro.th2.codec.openapi.json.visitor.encode

import com.exactpro.th2.codec.openapi.assertBoolean
import com.exactpro.th2.codec.openapi.assertFloat
import com.exactpro.th2.codec.openapi.assertInteger
import com.exactpro.th2.codec.openapi.assertString
import com.exactpro.th2.codec.openapi.OpenApiCodecSettings
import com.exactpro.th2.codec.openapi.writer.SchemaWriter
import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonObjectVisitor
import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.value.toValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.exactpro.th2.codec.openapi.createArrayTestSchema
import com.exactpro.th2.codec.openapi.createTestSchema
import com.exactpro.th2.codec.openapi.utils.getResourceAsText
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import org.junit.jupiter.api.Assertions
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
        val includedObjectValue = message().addField(stringName, stringValue).build()

        val message = message().addField(fieldName, message().apply {
            addField(stringName, stringValue)
            addField(integerName, integerValue)
            addField(booleanName, booleanValue)
            addField(floatName, floatValue)
            addField(includedObject, includedObjectValue)
        }).build()

        val result = EncodeJsonObjectVisitor(message).apply {
            visit(fieldName, null as? Schema<*>, openAPI.components.schemas["ObjectTest"]!!, true, SchemaWriter(openAPI))
        }.getResult().toStringUtf8()

        mapper.readTree(result).get(fieldName).let { objectNode ->
            objectNode.assertString(stringName, stringValue)
            objectNode.assertInteger(integerName, integerValue)
            objectNode.assertBoolean(booleanName, booleanValue)
            objectNode.assertFloat(floatName, floatValue)
            objectNode.get(includedObject).assertString(stringName, stringValue)
        }

    }

    @Test
    fun `string test encode`() {
        val fieldName = "stringField"
        val simpleValue = "stringValue"
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? String, schema, true)
        val result = mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName)?.asText()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `boolean test encode`() {
        val fieldName = "booleanField"
        val simpleValue = true
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Boolean, schema, true)
        val result = mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName)?.asBoolean()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `int test encode`() {
        val fieldName = "intField"
        val simpleValue = 123
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Int, schema, true)
        val result = mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName)?.asInt()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `float test encode`() {
        val fieldName = "floatField"
        val simpleValue = 123.1f
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Float, schema, true)
        val result = mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName)?.asText()?.toFloat()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `double test encode`() {
        val fieldName = "doubleField"
        val simpleValue = 123.1
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Double, schema, true)
        val result = mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName)?.asDouble()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `long test encode`() {
        val fieldName = "longField"
        val simpleValue = 123123L
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Long, schema, true)
        val result = mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName)?.asLong()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `string array test encode`() {
        val fieldName = "stringField"
        val collection = listOf("stringValue1", "stringValue2", "stringValue3", "stringValue4")
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("string")
        visitor.visitStringCollection(fieldName, null, schema, true)
        val result = requireNotNull(mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName) as? ArrayNode)
        Assertions.assertEquals(4, result.size())
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asText())
        }
    }

    @Test
    fun `boolean array test encode`() {
        val fieldName = "booleanField"
        val collection = listOf(true, false, false, true)
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("boolean")
        visitor.visitBooleanCollection(fieldName, null, schema, true)
        val result = requireNotNull(mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName) as? ArrayNode)
        Assertions.assertEquals(4, result.size())
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asBoolean())
        }
    }

    @Test
    fun `int array test encode`() {
        val fieldName = "intField"
        val collection = listOf(1, 2, 2, 4)
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("integer")
        visitor.visitIntegerCollection(fieldName, null, schema, true)
        val result = requireNotNull(mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName) as? ArrayNode)
        Assertions.assertEquals(4, result.size())
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asInt())
        }
    }

    @Test
    fun `float array test encode`() {
        val fieldName = "floatField"
        val collection = listOf(0.1f, 0.2f, 0.2f, 0.4f)
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("number","float")
        visitor.visitFloatCollection(fieldName, null, schema, true)
        val result = requireNotNull(mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName) as? ArrayNode)
        Assertions.assertEquals(4, result.size())
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asText().toFloat())
        }
    }

    @Test
    fun `double array test encode`() {
        val fieldName = "doubleField"
        val collection = listOf(0.1, 0.2, 0.2, 0.4)
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("number", "double")
        visitor.visitDoubleCollection(fieldName, null , schema, true)
        val result = requireNotNull(mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName) as? ArrayNode)
        Assertions.assertEquals(4, result.size())
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asDouble())
        }
    }

    @Test
    fun `long array test encode`() {
        val fieldName = "longField"
        val collection = listOf(111111111L, 222222222L, 222222222L, 444444444L)
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("integer", "int64")
        visitor.visitLongCollection(fieldName, null, schema, true)
        val result = requireNotNull(mapper.readTree(visitor.getResult().toStringUtf8()).get(fieldName) as? ArrayNode)
        Assertions.assertEquals(4, result.size())
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asLong())
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
        val includedObjectValue = message().addField(stringName, stringValue).build()

        val listValue = ListValue.newBuilder().apply {
            addValues(message().addField(stringName, stringValue).build().toValue())
            addValues(message().addField(integerName, integerValue).build().toValue())
            addValues(message().addField(booleanName, booleanValue).build().toValue())
            addValues(message().addField(floatName, floatValue).build().toValue())
            addValues(message().apply {
                addField(stringName, stringValue)
                addField(integerName, integerValue)
                addField(booleanName, booleanValue)
                addField(floatName, floatValue)
                addField(includedObject, includedObjectValue)
            }.build().toValue())
        }

        val message = message().addField(fieldName, listValue).build()

        val result = EncodeJsonObjectVisitor(message).apply {
            visitObjectCollection(fieldName, null, openAPI.components.schemas["ArrayObjectTest"]!! as ArraySchema, true, SchemaWriter(openAPI))
        }.getResult().toStringUtf8()

        mapper.readTree(result).let { objectNode ->
            (objectNode[fieldName] as ArrayNode).let { arrayNode->
                Assertions.assertEquals(5, arrayNode.size())
                arrayNode.get(0).assertString(stringName, stringValue)
                arrayNode.get(1).assertInteger(integerName, integerValue)
                arrayNode.get(2).assertBoolean(booleanName, booleanValue)
                arrayNode.get(3).assertFloat(floatName, floatValue)
                arrayNode.get(4).let { bigMessage ->
                    bigMessage.assertString(stringName, stringValue)
                    bigMessage.assertInteger(integerName, integerValue)
                    bigMessage.assertBoolean(booleanName, booleanValue)
                    bigMessage.assertFloat(floatName, floatValue)
                    bigMessage.get(includedObject).assertString(stringName, stringValue)
                }
            }
        }
    }

    private companion object {
        val openAPI: OpenAPI = OpenAPIParser().readContents(getResourceAsText("dictionaries/valid/visitorTests.yml"), null, OpenApiCodecSettings().dictionaryParseOption).openAPI
        val mapper = ObjectMapper()
    }
}