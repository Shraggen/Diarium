package com.shraggen.diarium.schema

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object JsonSchemaSerializer {

    fun serialize(schema: JsonSchema): JsonObject =
        when (schema) {
            is JsonObjectSchema -> serializeObject(schema)
            is JsonStringSchema -> serializeString(schema)
            is JsonIntegerSchema -> serializeInteger(schema)
            is JsonBooleanSchema -> serializeBoolean(schema)
            is JsonArraySchema -> serializeArray(schema)
        }

    private fun serializeObject(schema: JsonObjectSchema): JsonObject =
        buildJsonObject {
            put("type", "object")
            schema.description?.let { put("description", it) }
            put(
                "properties",
                buildJsonObject {
                    schema.properties.forEach { (name, child) ->
                        put(name, serialize(child))
                    }
                },
            )
            put(
                "required",
                JsonArray(schema.required.map(::JsonPrimitive)),
            )
        }

    private fun serializeString(schema: JsonStringSchema): JsonObject =
        buildJsonObject {
            put("type", "string")
            schema.description?.let { put("description", it) }
            if (schema.enum.isNotEmpty()) {
                put(
                    "enum",
                    JsonArray(schema.enum.map(::JsonPrimitive)),
                )
            }
        }

    private fun serializeInteger(schema: JsonIntegerSchema): JsonObject =
        buildJsonObject {
            put("type", "integer")
            schema.description?.let { put("description", it) }
            schema.minimum?.let { put("minimum", it) }
            schema.maximum?.let { put("maximum", it) }
        }

    private fun serializeBoolean(schema: JsonBooleanSchema): JsonObject =
        buildJsonObject {
            put("type", "boolean")
            schema.description?.let { put("description", it) }
        }

    private fun serializeArray(schema: JsonArraySchema): JsonObject =
        buildJsonObject {
            put("type", "array")
            schema.description?.let { put("description", it) }
            put("items", serialize(schema.items))
        }
}
