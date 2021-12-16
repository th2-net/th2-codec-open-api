package visitor

import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonObjectVisitor
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.ObjectMapper
import createTestSchema
import io.swagger.v3.oas.models.media.StringSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JsonObjectVisitorTest {

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

    private companion object {
        val mapper = ObjectMapper()
    }
}