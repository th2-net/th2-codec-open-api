package visitor

import com.exactpro.th2.codec.openapi.utils.putAll
import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonArrayVisitor
import com.exactpro.th2.codec.openapi.writer.visitors.json.EncodeJsonArrayVisitor
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.getList
import com.exactpro.th2.common.message.message
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import createArrayTestSchema
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DecodeJsonArrayTest {

    @Test
    fun `not supported decode`() {
        val node = mapper.createArrayNode()
        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            DecodeJsonArrayVisitor(node).visit("", null as? String, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            DecodeJsonArrayVisitor(node).visit("", null as? Int, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            DecodeJsonArrayVisitor(node).visit("", null as? Double, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            DecodeJsonArrayVisitor(node).visit("", null as? Long, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            DecodeJsonArrayVisitor(node).visit("", null as? Float, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            DecodeJsonArrayVisitor(node).visit("", null as? Boolean, StringSchema(), true)
        }

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            DecodeJsonArrayVisitor(node).visit("", null as? Schema<*>, StringSchema(), OpenAPI(), true)
        }
    }

    @Test
    fun `string array test decode`() {
        val fieldName = "stringField"
        val collection = listOf("stringValue1", "stringValue2", "stringValue3", "stringValue4")
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode)
        visitor.visitStringCollection(fieldName, null, createArrayTestSchema("string"), true)
        val result = requireNotNull(visitor.getResult().getList(fieldName))
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result[index].simpleValue)
        }
    }

    @Test
    fun `boolean array test decode`() {
        val fieldName = "booleanField"
        val collection = listOf(true, false, false, true)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode)
        visitor.visitBooleanCollection(fieldName, null, createArrayTestSchema("boolean"), true)
        val result = requireNotNull(visitor.getResult().getList(fieldName))
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result[index].simpleValue.toBoolean())
        }
    }

    @Test
    fun `integer array test decode`() {
        val fieldName = "intField"
        val collection = listOf(1, 2, 2, 4)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode)
        visitor.visitIntegerCollection(fieldName, null, createArrayTestSchema("integer"), true)
        val result = requireNotNull(visitor.getResult().getList(fieldName))
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result[index].simpleValue.toInt())
        }
    }

    @Test
    fun `double array test decode`() {
        val fieldName = "doubleField"
        val collection = listOf(1.1, 2.2, 2.1, 4.3)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode)
        visitor.visitDoubleCollection(fieldName, null, createArrayTestSchema("number", "double"), true)
        val result = requireNotNull(visitor.getResult().getList(fieldName))
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result[index].simpleValue.toDouble())
        }
    }

    @Test
    fun `float array test decode`() {
        val fieldName = "floatField"
        val collection = listOf(1.1f, 2.2f, 2.1f, 4.3f)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode)
        visitor.visitFloatCollection(fieldName, null, createArrayTestSchema("number", "float"), true)
        val result = requireNotNull(visitor.getResult().getList(fieldName))
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result[index].simpleValue.toFloat())
        }
    }

    @Test
    fun `long array test decode`() {
        val fieldName = "longField"
        val collection = listOf(1111121111L, 2222222222L, 2111331111L, 433333223333L)
        val jsonArrayNode = mapper.createArrayNode().apply {
            collection.forEach(this::add)
        }
        val visitor = DecodeJsonArrayVisitor(jsonArrayNode)
        visitor.visitLongCollection(fieldName, null, createArrayTestSchema("integer", "int64"), true)
        val result = requireNotNull(visitor.getResult().getList(fieldName))
        collection.forEachIndexed { index, value ->
            Assertions.assertEquals(value, result[index].simpleValue.toLong())
        }
    }

    private companion object {
        private val mapper = ObjectMapper()
    }
}