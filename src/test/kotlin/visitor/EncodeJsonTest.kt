package visitor

import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonObjectVisitor
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import createArrayTestSchema
import createTestSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EncodeJsonTest {

    @Test
    fun `string test encode`() {
        val fieldName = "stringField"
        val simpleValue = "stringValue"
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? String, schema, true)
        val result = mapper.readTree(visitor.getResult()).get(fieldName)?.asText()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `boolean test encode`() {
        val fieldName = "booleanField"
        val simpleValue = true
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Boolean, schema, true)
        val result = mapper.readTree(visitor.getResult()).get(fieldName)?.asBoolean()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `int test encode`() {
        val fieldName = "intField"
        val simpleValue = 123
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Int, schema, true)
        val result = mapper.readTree(visitor.getResult()).get(fieldName)?.asInt()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `float test encode`() {
        val fieldName = "floatField"
        val simpleValue = 123.1f
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Float, schema, true)
        val result = mapper.readTree(visitor.getResult()).get(fieldName)?.asText()?.toFloat()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `double test encode`() {
        val fieldName = "doubleField"
        val simpleValue = 123.1
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Double, schema, true)
        val result = mapper.readTree(visitor.getResult()).get(fieldName)?.asDouble()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `long test encode`() {
        val fieldName = "longField"
        val simpleValue = 123123L
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, simpleValue).build())
        val schema = createTestSchema(simpleValue)
        visitor.visit(fieldName, null as? Long, schema, true)
        val result = mapper.readTree(visitor.getResult()).get(fieldName)?.asLong()
        Assertions.assertEquals(simpleValue, result)
    }

    @Test
    fun `string array test encode`() {
        val fieldName = "stringField"
        val collection = listOf("stringValue1", "stringValue2", "stringValue3", "stringValue4")
        val visitor = EncodeJsonObjectVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("string")
        visitor.visitStringCollection(fieldName, null, schema, true)
        val result = requireNotNull(mapper.readTree(visitor.getResult()).get(fieldName) as? ArrayNode)
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
        val result = requireNotNull(mapper.readTree(visitor.getResult()).get(fieldName) as? ArrayNode)
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
        val result = requireNotNull(mapper.readTree(visitor.getResult()).get(fieldName) as? ArrayNode)
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
        val result = requireNotNull(mapper.readTree(visitor.getResult()).get(fieldName) as? ArrayNode)
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
        val result = requireNotNull(mapper.readTree(visitor.getResult()).get(fieldName) as? ArrayNode)
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
        val result = requireNotNull(mapper.readTree(visitor.getResult()).get(fieldName) as? ArrayNode)
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asLong())
        }
    }

    private companion object {
        val mapper = ObjectMapper()
    }
}