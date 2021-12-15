import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.apache.poi.ss.formula.functions.T


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
