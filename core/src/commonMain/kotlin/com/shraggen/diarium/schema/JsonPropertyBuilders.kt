package com.shraggen.diarium.schema

class JsonStringBuilder {

    private var description: String? = null

    private val enum = mutableListOf<String>()

    fun description(text: String) {
        description = text
    }

    fun enum(vararg values: String) {
        enum += values
    }

    internal fun build() =
        JsonStringSchema(
            description = description,
            enum = enum,
        )
}

class JsonIntegerBuilder {

    private var description: String? = null

    private var minimum: Int? = null

    private var maximum: Int? = null

    fun description(text: String) {
        description = text
    }

    fun minimum(value: Int) {
        minimum = value
    }

    fun maximum(value: Int) {
        maximum = value
    }

    internal fun build() =
        JsonIntegerSchema(
            description,
            minimum,
            maximum,
        )
}

class JsonBooleanBuilder {

    private var description: String? = null

    fun description(text: String) {
        description = text
    }

    internal fun build() =
        JsonBooleanSchema(description)
}

class JsonArrayBuilder(
    private val items: JsonSchema,
) {

    private var description: String? = null

    fun description(text: String) {
        description = text
    }

    internal fun build() =
        JsonArraySchema(
            items,
            description,
        )
}
