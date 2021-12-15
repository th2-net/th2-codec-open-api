package visitor

import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonArrayVisitor
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.StringSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JsonArrayVisitorTest {
    @Test
    fun `string test encode`() {
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
    fun `boolean test encode`() {
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
    fun `int test encode`() {
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
    fun `float test encode`() {
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
    fun `double test encode`() {
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
    fun `long test encode`() {
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

    private fun createArrayTestSchema(type: String, format: String? = null) : ArraySchema {
        return ArraySchema().apply {
            items = StringSchema()
            items.type = type
            format?.let {
                items.format = format
            }
        }
    }

    private companion object {
        val mapper = ObjectMapper()
    }
}