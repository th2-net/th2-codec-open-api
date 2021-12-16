package visitor.encode

import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonArrayVisitor
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import createArrayTestSchema
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JsonArrayVisitorTest {

    @Test
    fun `not supported encode`() {
        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            EncodeJsonArrayVisitor(message().build()).visit("", null as? String, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            EncodeJsonArrayVisitor(message().build()).visit("", null as? Int, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            EncodeJsonArrayVisitor(message().build()).visit("", null as? Double, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            EncodeJsonArrayVisitor(message().build()).visit("", null as? Long, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            EncodeJsonArrayVisitor(message().build()).visit("", null as? Float, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            EncodeJsonArrayVisitor(message().build()).visit("", null as? Boolean, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            EncodeJsonArrayVisitor(message().build()).visit("", null as? Schema<*>, StringSchema(), OpenAPI(), true)
        }
    }

    @Test
    fun `string array test encode`() {
        val fieldName = "stringField"
        val collection = listOf("stringValue1", "stringValue2", "stringValue3", "stringValue4")
        val visitor = EncodeJsonArrayVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("string")
        visitor.visitStringCollection(fieldName, null, schema, true)
        val result = (mapper.readTree(visitor.getResult()) as ArrayNode)
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asText())
        }
    }

    @Test
    fun `boolean array test encode`() {
        val fieldName = "booleanField"
        val collection = listOf(true, false, false, true)
        val visitor = EncodeJsonArrayVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("boolean")
        visitor.visitBooleanCollection(fieldName, null, schema, true)
        val result = (mapper.readTree(visitor.getResult()) as ArrayNode)
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asBoolean())
        }
    }

    @Test
    fun `int array test encode`() {
        val fieldName = "intField"
        val collection = listOf(1, 2, 2, 4)
        val visitor = EncodeJsonArrayVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("integer")
        visitor.visitIntegerCollection(fieldName, null, schema, true)
        val result = (mapper.readTree(visitor.getResult()) as ArrayNode)
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asInt())
        }
    }

    @Test
    fun `float array test encode`() {
        val fieldName = "floatField"
        val collection = listOf(0.1f, 0.2f, 0.2f, 0.4f)
        val visitor = EncodeJsonArrayVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("number","float")
        visitor.visitFloatCollection(fieldName, null, schema, true)
        val result = (mapper.readTree(visitor.getResult()) as ArrayNode)
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asText().toFloat())
        }
    }

    @Test
    fun `double array test encode`() {
        val fieldName = "doubleField"
        val collection = listOf(0.1, 0.2, 0.2, 0.4)
        val visitor = EncodeJsonArrayVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("number", "double")
        visitor.visitDoubleCollection(fieldName, null , schema, true)
        val result = (mapper.readTree(visitor.getResult()) as ArrayNode)
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asDouble())
        }
    }

    @Test
    fun `long array test encode`() {
        val fieldName = "longField"
        val collection = listOf(111111111L, 222222222L, 222222222L, 444444444L)
        val visitor = EncodeJsonArrayVisitor(message().addField(fieldName, collection).build())
        val schema = createArrayTestSchema("integer", "int64")
        visitor.visitLongCollection(fieldName, null, schema, true)
        val result = (mapper.readTree(visitor.getResult()) as ArrayNode)
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result.get(index).asLong())
        }
    }

    private companion object {
        val mapper = ObjectMapper()
    }
}