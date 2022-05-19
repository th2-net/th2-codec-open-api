package com.exactpro.th2.codec.openapi.writer.visitors.json.updated

import com.exactpro.th2.codec.openapi.utils.checkEnum
import com.exactpro.th2.codec.openapi.utils.getBoolean
import com.exactpro.th2.codec.openapi.utils.getField
import com.exactpro.th2.codec.openapi.utils.putAll
import com.exactpro.th2.codec.openapi.writer.visitors.UpdatedSchemaVisitor
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.value.getBigDecimal
import com.exactpro.th2.common.value.getList
import com.exactpro.th2.common.value.getLong
import com.exactpro.th2.common.value.getMessage
import com.exactpro.th2.common.value.getString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.ByteString
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import java.math.BigDecimal

open class EncodeJsonObjectVisitor(override val from: Message, override val openAPI: OpenAPI) : UpdatedSchemaVisitor.EncodeVisitor<Message>() {
    private val rootNode: ObjectNode = mapper.createObjectNode()

    override fun visit(fieldName: String, fldStruct: ObjectSchema, required: Boolean, throwUndefined: Boolean) {
        from.getField(fieldName, required)?.getMessage()?.let { message ->
            val visitor = EncodeJsonObjectVisitor(message, openAPI)
            for (entry in fldStruct.properties) {
                when (val propertySchema = entry.value) {
                    is NumberSchema -> visitor.visit(entry.key, propertySchema, fldStruct.required.contains(entry.key))
                    is IntegerSchema -> visitor.visit(entry.key, propertySchema, fldStruct.required.contains(entry.key))
                    is BooleanSchema -> visitor.visit(entry.key, propertySchema, fldStruct.required.contains(entry.key))
                    is StringSchema -> visitor.visit(entry.key, propertySchema, fldStruct.required.contains(entry.key))
                    is ArraySchema -> visitor.visit(entry.key, propertySchema, fldStruct.required.contains(entry.key))
                    is ObjectSchema -> visitor.visit(entry.key, propertySchema, fldStruct.required.contains(entry.key))
                    is ComposedSchema -> visitor.visit(entry.key, propertySchema, fldStruct.required.contains(entry.key))
                    else -> error("Unsupported type of schema [${propertySchema}] for object visitor - visit object schema")
                }
            }
            if (throwUndefined) {
                val propertyNames = fldStruct.properties.keys
                val undefinedFields = from.fieldsMap.keys.filter { !propertyNames.contains(it) }
                if (undefinedFields.isNotEmpty()) {
                    error("Found undefined fields: " + undefinedFields.joinToString(", "))
                }
            }
            rootNode.set<ObjectNode>(fieldName, visitor.rootNode)
        } ?: fldStruct.default?.let { error("Default values isn't supported for objects") }
    }

    override fun visit(fieldName: String, fldStruct: ArraySchema, required: Boolean) {
        val itemSchema = fldStruct.items

        from.getField(fieldName, required)?.getList()?.let { listOfValues ->
            when (itemSchema) {
                is NumberSchema -> rootNode.putArray(fieldName).putAll<BigDecimal>(listOfValues)
                is IntegerSchema -> rootNode.putArray(fieldName).putAll<Long>(listOfValues)
                is BooleanSchema -> rootNode.putArray(fieldName).putAll<Boolean>(listOfValues)
                is StringSchema -> rootNode.putArray(fieldName).putAll<String>(listOfValues)
                is ObjectSchema -> rootNode.putArray(fieldName).run {
                    val listOfNodes = mutableListOf<ObjectNode>()
                    listOfValues.forEach {
                        EncodeJsonObjectVisitor(checkNotNull(it.getMessage()) {" Value from list [$fieldName] must be message"}, openAPI).let { visitor ->
                            visitor.visit("message", itemSchema, true)
                            visitor.rootNode.get("message")?.run {
                                listOfNodes.add(this as ObjectNode)
                            }
                        }
                    }
                    listOfNodes.forEach { add(it) }
                }
                else -> error("Unsupported items type: ${fldStruct.items::class.java}")
            }
        } ?: fldStruct.default?.let { error("Default values isn't supported for arrays") }
    }

    override fun visit(fieldName: String, fldStruct: ComposedSchema, required: Boolean) {
        TODO("Not yet implemented")
    }

    override fun visit(fieldName: String, fldStruct: NumberSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required), fldStruct, Value::getBigDecimal) {
        rootNode.put(fieldName, it)
    }

    override fun visit(fieldName: String, fldStruct: IntegerSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required), fldStruct, Value::getLong) {
        rootNode.put(fieldName, it.toLong())
    }

    override fun visit(fieldName: String, fldStruct: StringSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required), fldStruct, Value::getString) {
        rootNode.put(fieldName, it)
    }

    override fun visit(fieldName: String, fldStruct: BooleanSchema, required: Boolean) = visitPrimitive(fieldName, from.getField(fieldName, required), fldStruct, Value::getBoolean) {
        rootNode.put(fieldName, it)
    }

    private inline fun <T> visitPrimitive(fieldName: String, fieldValue: Value?, fldStruct: Schema<T>, convert: (Value) -> T, put: (T) -> Unit) {
        fieldValue?.run(convert)?.let { value ->
            fldStruct.checkEnum(value, fieldName)
            put(value)
        } ?: fldStruct.default?.run(put)
    }

    override fun getResult(): ByteString = ByteString.copyFrom(rootNode.toString().toByteArray())

    fun getNode() = rootNode

    private companion object {
        val mapper = ObjectMapper().apply {
            nodeFactory = JsonNodeFactory.withExactBigDecimals(true)
        }
    }

}