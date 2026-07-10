package com.shraggen.diarium.schema

sealed interface JsonSchema

data class JsonObjectSchema(
    val properties: Map<String, JsonSchema>,
    val required: Set<String>,
    val description: String? = null,
) : JsonSchema

data class JsonStringSchema(
    val description: String? = null,
    val enum: List<String> = emptyList(),
) : JsonSchema

data class JsonIntegerSchema(
    val description: String? = null,
    val minimum: Int? = null,
    val maximum: Int? = null,
) : JsonSchema

data class JsonBooleanSchema(
    val description: String? = null,
) : JsonSchema

data class JsonArraySchema(
    val items: JsonSchema,
    val description: String? = null,
) : JsonSchema
