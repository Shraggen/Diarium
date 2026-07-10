package com.shraggen.diarium.schema

import kotlinx.serialization.json.*

object JsonSchemaSerializer {

    fun serialize(schema: JsonSchema): JsonObject =
        when (schema) {

            is JsonObjectSchema ->
                buildJsonObject {

                    put("type", "object")

                    if (schema.description != null) {
                        put("description", schema.description)
                    }

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
                        JsonArray(
                            schema.required.map(::JsonPrimitive),
                        ),
                    )
                }

            is JsonStringSchema ->
                buildJsonObject {

                    put("type", "string")

                    schema.description?.let {
                        put("description", it)
                    }

                    if (schema.enum.isNotEmpty()) {

                        put(
                            "enum",
                            JsonArray(schema.enum.map(::JsonPrimitive)),
                        )
                    }
                }

            is JsonIntegerSchema ->
                buildJsonObject {

                    put("type", "integer")

                    schema.description?.let {
                        put("description", it)
                    }

                    schema.minimum?.let {
                        put("minimum", it)
                    }

                    schema.maximum?.let {
                        put("maximum", it)
                    }
                }

            is JsonBooleanSchema ->
                buildJsonObject {

                    put("type", "boolean")

                    schema.description?.let {
                        put("description", it)
                    }
                }

            is JsonArraySchema ->
                buildJsonObject {

                    put("type", "array")

                    schema.description?.let {
                        put("description", it)
                    }

                    put(
                        "items",
                        serialize(schema.items),
                    )
                }
        }
}
