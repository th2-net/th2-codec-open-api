package visitor

import com.exactpro.th2.codec.openapi.writer.visitors.json.DecodeJsonObjectVisitor
import com.exactpro.th2.common.assertDouble
import com.exactpro.th2.common.assertInt
import com.exactpro.th2.common.assertList
import com.exactpro.th2.common.assertString
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.value.toValue
import com.fasterxml.jackson.databind.ObjectMapper
import createArrayTestSchema
import createTestSchema
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DecodeJsonTest {

    @Test
    fun `string test decode`() {
        val fieldName = "stringField"
        val simpleValue = "stringValue"
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visit(fieldName, null as? String, createTestSchema(simpleValue), true)
        }.getResult()
        result.assertString(fieldName, simpleValue)
    }

    @Test
    fun `boolean test decode`() {
        val fieldName = "booleanField"
        val simpleValue = false
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visit(fieldName, null as? Boolean, createTestSchema(simpleValue), true)
        }.getResult()
        Assertions.assertEquals(simpleValue, result.getString(fieldName).toBoolean())
    }

    @Test
    fun `int test decode`() {
        val fieldName = "intField"
        val simpleValue = 12345
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visit(fieldName, null as? Int, createTestSchema(simpleValue), true)
        }.getResult()
        result.assertInt(fieldName, simpleValue)
    }

    @Test
    fun `double test decode`() {
        val fieldName = "doubleField"
        val simpleValue = 12345.67
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visit(fieldName, null as? Double, createTestSchema(simpleValue), true)
        }.getResult()
        result.assertDouble(fieldName, simpleValue)
    }

    @Test
    fun `float test decode`() {
        val fieldName = "floatField"
        val simpleValue = 12345.12f
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visit(fieldName, null as? Float, createTestSchema(simpleValue), true)
        }.getResult()
        Assertions.assertEquals(simpleValue, result.getString(fieldName)?.toFloat())
    }

    @Test
    fun `long test decode`() {
        val fieldName = "longField"
        val simpleValue = 1234512345L
        val json =  mapper.createObjectNode().apply {
            put(fieldName, simpleValue)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visit(fieldName, null as? Long, createTestSchema(simpleValue), true)
        }.getResult()
        Assertions.assertEquals(simpleValue, result.getString(fieldName)?.toLong())
    }

    @Test
    fun `string array test decode`() {
        val fieldName = "stringArrayField"
        val collection = listOf("stringValue1", "stringValue3", "stringValue2", "stringValue4")
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visitStringCollection(fieldName, null, createArrayTestSchema("string"), true)
        }.getResult()
        result.assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `boolean array test decode`() {
        val fieldName = "booleanArrayField"
        val collection = listOf(true, false, false, true)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visitBooleanCollection(fieldName, null, createArrayTestSchema("string"), true)
        }.getResult()
        result.assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `int array test decode`() {
        val fieldName = "intArrayField"
        val collection = listOf(1, 3, 2, 4)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visitIntegerCollection(fieldName, null, createArrayTestSchema("integer"), true)
        }.getResult()
        result.assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `double array test decode`() {
        val fieldName = "doubleArrayField"
        val collection = listOf(0.1, 0.1, 0.3, 0.2)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visitDoubleCollection(fieldName, null, createArrayTestSchema("string"), true)
        }.getResult()
        result.assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `float array test decode`() {
        val fieldName = "floatArrayField"
        val collection = listOf(0.1f, 0.1f, 0.3f, 0.2f)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visitFloatCollection(fieldName, null, createArrayTestSchema("string"), true)
        }.getResult()
        result.assertList(fieldName, collection.map { it.toValue() })
    }

    @Test
    fun `long array test decode`() {
        val fieldName = "longArrayField"
        val collection = listOf(123123123L, 321312321L, 333333333L, 444444444L)
        val json =  mapper.createObjectNode().apply {
            val arrayNode = putArray(fieldName)
            collection.forEach(arrayNode::add)
        }
        val result = DecodeJsonObjectVisitor(json).apply {
            visitLongCollection(fieldName, null, createArrayTestSchema("string"), true)
        }.getResult()
        result.assertList(fieldName, collection.map { it.toValue() })
    }


    private companion object {
        private val mapper = ObjectMapper()
    }
}