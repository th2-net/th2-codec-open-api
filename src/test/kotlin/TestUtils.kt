import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import io.swagger.v3.oas.models.media.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail


fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path).readText()
}

inline fun <reified T:Any>createTestSchema(value: T?, fillEnum: List<T>? = null) : Schema<*> {
    when (T::class) {
        String::class -> {
            return StringSchema().apply {
                type = "string"
                example = value
                fillEnum?.forEach {
                    enum.add(it.toString())
                }
            }
        }
        Boolean::class -> {
            return BooleanSchema().apply {
                type = "boolean"
                example = value
            }
        }
        Int::class -> {
            return NumberSchema().apply {
                type = "number"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Int).toBigDecimal())
                }
            }
        }
        Long::class -> {
            return NumberSchema().apply {
                type = "number"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Long).toBigDecimal())
                }
            }
        }
        Float::class -> {
            return NumberSchema().apply {
                type = "number"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Float).toBigDecimal())
                }
            }
        }
        Double::class -> {
            return NumberSchema().apply {
                type = "number"
                example = value
                fillEnum?.forEach {
                    enum.add((it as Double).toBigDecimal())
                }
            }
        }
        else -> {
            throw UnsupportedOperationException("createTestSchema don't supports ${T::class} type of value")
        }
    }
}

fun createArrayTestSchema(type: String, format: String? = null) : ArraySchema {
    return ArraySchema().apply {
        items = StringSchema()
        items.type = type
        format?.let {
            items.format = format
        }
    }
}

fun JsonNode.asSchema() : Schema<*> {
    return when (this.nodeType) {
        JsonNodeType.ARRAY -> ArraySchema().apply {
            val arrayNode = this@asSchema as ArrayNode
            if (arrayNode.get(0).isObject) {
                this.items = arrayNode.map { it.asSchema() }.reduce { to, from  -> to.apply { properties = properties + from.properties } }
            } else {
                this.items = arrayNode.get(0).asSchema()
            }
        }
        JsonNodeType.OBJECT -> ObjectSchema().apply {
            properties = mutableMapOf()
            fields().forEach {
                properties[it.key] = it.value.asSchema()
            }
        }
        JsonNodeType.NUMBER -> {
            if (this.isFloat) {
                NumberSchema().apply {
                    format = "float"
                    example = this@asSchema.asInt()
                }
            } else if (this.isDouble) {
                NumberSchema().apply {
                    format = "double"
                    example = this@asSchema.asDouble()
                }
            } else if (this.isInt) {
                IntegerSchema().apply {
                    example = this@asSchema.asInt()
                }
            } else if (this.isLong) {
                IntegerSchema().apply {
                    type = "int64"
                    example = this@asSchema.asInt()
                }
            } else error("Wrong type of number inside json")
        }
        JsonNodeType.STRING -> {
            StringSchema().apply {
                example = this@asSchema.asText()
            }
        }
        JsonNodeType.BOOLEAN -> {
            BooleanSchema().apply {
                example = this@asSchema.asBoolean()
            }
        }
        JsonNodeType.NULL -> {
            StringSchema()
        }
        else -> {
            error("Wrong JsonNodeType of number inside json")
        }
    }
}

fun JsonNode.assertString(fieldName: String, fieldValue: String) {
    if (!get(fieldName).isTextual) {
        fail("$fieldName isn't textual type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asText())
}

fun JsonNode.assertBoolean(fieldName: String, fieldValue: Boolean) {
    if (!get(fieldName).isBoolean) {
        fail("$fieldName isn't boolean type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asBoolean())
}

fun JsonNode.assertFloat(fieldName: String, fieldValue: Float) {
    if (!get(fieldName).isNumber) {
        fail("$fieldName isn't float type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asText().toFloat())
}

fun JsonNode.assertDouble(fieldName: String, fieldValue: Double) {
    if (!get(fieldName).isNumber) {
        fail("$fieldName isn't double type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asDouble())
}

fun JsonNode.assertLong(fieldName: String, fieldValue: Long) {
    if (!get(fieldName).isNumber) {
        fail("$fieldName isn't long type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asLong())
}

fun JsonNode.assertInteger(fieldName: String, fieldValue: Int) {
    if (!get(fieldName).isNumber) {
        fail("$fieldName isn't integer type: ${get(fieldName).asText()}")
    }
    Assertions.assertEquals(fieldValue, get(fieldName).asInt())
}