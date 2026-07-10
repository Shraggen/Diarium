package com.shraggen.diarium.schema

fun obj(block: JsonObjectBuilder.() -> Unit): JsonObjectSchema =
    JsonObjectBuilder().apply(block).build()

class JsonObjectBuilder {

    private val properties = linkedMapOf<String, JsonSchema>()

    private val required = linkedSetOf<String>()

    private var description: String? = null

    fun description(text: String) {
        description = text
    }

    fun required(vararg names: String) {
        required += names
    }

    fun string(
        name: String,
        block: JsonStringBuilder.() -> Unit = {},
    ) {
        properties[name] = JsonStringBuilder()
            .apply(block)
            .build()
    }

    fun integer(
        name: String,
        block: JsonIntegerBuilder.() -> Unit = {},
    ) {
        properties[name] = JsonIntegerBuilder()
            .apply(block)
            .build()
    }

    fun boolean(
        name: String,
        block: JsonBooleanBuilder.() -> Unit = {},
    ) {
        properties[name] = JsonBooleanBuilder()
            .apply(block)
            .build()
    }

    fun array(
        name: String,
        items: JsonSchema,
        block: JsonArrayBuilder.() -> Unit = {},
    ) {
        properties[name] = JsonArrayBuilder(items)
            .apply(block)
            .build()
    }

    fun objectProperty(
        name: String,
        block: JsonObjectBuilder.() -> Unit,
    ) {
        properties[name] = obj(block)
    }

    internal fun build() =
        JsonObjectSchema(
            properties = properties.toMap(),
            required = required.toSet(),
            description = description,
        )

    fun stringRequired(
        name: String,
        block: JsonStringBuilder.() -> Unit = {},
    ) {
        string(name, block)
        required(name)
    }

    fun integerRequired(
        name: String,
        block: JsonIntegerBuilder.() -> Unit = {},
    ) {
        integer(name, block)
        required(name)
    }

    fun booleanRequired(
        name: String,
        block: JsonBooleanBuilder.() -> Unit = {},
    ) {
        boolean(name, block)
        required(name)
    }
}
