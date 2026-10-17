package com.shraggen.diarium.schema

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonSchemaPropertyTest {

    @Test
    fun serializedSchemaStatesExactlyTheModelledConstraints() = runTest {
        checkAll(PROPERTY_ITERATIONS, schemaArb(MAX_SCHEMA_DEPTH)) { schema ->
            assertSerializedFaithfully(schema, JsonSchemaSerializer.serialize(schema))
        }
    }

    @Test
    fun objectDslBuildsTheSchemaItDeclares() = runTest {
        checkAll(
            PROPERTY_ITERATIONS,
            Arb.list(declaredPropertyArb, 0..MAX_PROPERTIES),
            descriptionArb,
        ) { declarations, objectDescription ->
            val unique = declarations.distinctBy(DeclaredProperty::name)

            val built = obj {
                objectDescription?.let { description(it) }
                unique.forEach { it.declareIn(this) }
            }

            assertEquals(
                JsonObjectSchema(
                    properties = unique.associate { it.name to it.schema },
                    required = unique.filter(DeclaredProperty::required)
                        .map(DeclaredProperty::name)
                        .toSet(),
                    description = objectDescription,
                ),
                built,
            )
        }
    }
}

private fun assertSerializedFaithfully(schema: JsonSchema, json: JsonObject) {
    val expectedKeys = mutableSetOf("type")
    if (schema.declaredDescription != null) expectedKeys += "description"
    assertEquals(schema.declaredDescription, json["description"]?.jsonPrimitive?.content)

    when (schema) {
        is JsonObjectSchema -> {
            expectedKeys += setOf("properties", "required")
            assertEquals("object", json.type)
            val properties = json.getValue("properties").jsonObject
            assertEquals(schema.properties.keys.toList(), properties.keys.toList())
            schema.properties.forEach { (name, child) ->
                assertSerializedFaithfully(child, properties.getValue(name).jsonObject)
            }
            assertEquals(
                schema.required.toList(),
                json.getValue("required").jsonArray.map { it.jsonPrimitive.content },
            )
        }

        is JsonStringSchema -> {
            if (schema.enum.isNotEmpty()) expectedKeys += "enum"
            assertEquals("string", json.type)
            assertEquals(
                schema.enum.takeIf(List<String>::isNotEmpty),
                json["enum"]?.jsonArray?.map { it.jsonPrimitive.content },
            )
        }

        is JsonIntegerSchema -> {
            if (schema.minimum != null) expectedKeys += "minimum"
            if (schema.maximum != null) expectedKeys += "maximum"
            assertEquals("integer", json.type)
            assertEquals(schema.minimum, json["minimum"]?.jsonPrimitive?.int)
            assertEquals(schema.maximum, json["maximum"]?.jsonPrimitive?.int)
        }

        is JsonBooleanSchema -> assertEquals("boolean", json.type)

        is JsonArraySchema -> {
            expectedKeys += "items"
            assertEquals("array", json.type)
            assertSerializedFaithfully(schema.items, json.getValue("items").jsonObject)
        }
    }

    assertEquals(expectedKeys, json.keys)
}

private val JsonObject.type: String
    get() = getValue("type").jsonPrimitive.content

private val JsonSchema.declaredDescription: String?
    get() = when (val schema = this) {
        is JsonObjectSchema -> schema.description
        is JsonStringSchema -> schema.description
        is JsonIntegerSchema -> schema.description
        is JsonBooleanSchema -> schema.description
        is JsonArraySchema -> schema.description
    }

private class DeclaredProperty(
    val name: String,
    val schema: JsonSchema,
    val required: Boolean,
) {
    fun declareIn(builder: JsonObjectBuilder) {
        when (schema) {
            is JsonStringSchema -> {
                val block: JsonStringBuilder.() -> Unit = {
                    schema.description?.let { description(it) }
                    enum(*schema.enum.toTypedArray())
                }
                if (required) builder.stringRequired(name, block) else builder.string(name, block)
            }

            is JsonIntegerSchema -> {
                val block: JsonIntegerBuilder.() -> Unit = {
                    schema.description?.let { description(it) }
                    schema.minimum?.let { minimum(it) }
                    schema.maximum?.let { maximum(it) }
                }
                if (required) builder.integerRequired(name, block) else builder.integer(name, block)
            }

            is JsonBooleanSchema -> {
                val block: JsonBooleanBuilder.() -> Unit = {
                    schema.description?.let { description(it) }
                }
                if (required) builder.booleanRequired(name, block) else builder.boolean(name, block)
            }

            is JsonArraySchema -> {
                builder.array(name, schema.items) {
                    schema.description?.let { description(it) }
                }
                if (required) builder.required(name)
            }

            is JsonObjectSchema -> error("Nested objects are covered by serializer properties.")
        }
    }
}

private val nameArb = Arb.string(1..8)

private val descriptionArb = Arb.string(0..12).orNull()

private val stringSchemaArb: Arb<JsonSchema> = arbitrary {
    JsonStringSchema(
        description = descriptionArb.bind(),
        enum = Arb.list(Arb.string(0..6), 0..3).bind(),
    )
}

private val integerSchemaArb: Arb<JsonSchema> = arbitrary {
    JsonIntegerSchema(
        description = descriptionArb.bind(),
        minimum = Arb.int().orNull().bind(),
        maximum = Arb.int().orNull().bind(),
    )
}

private val booleanSchemaArb: Arb<JsonSchema> = arbitrary {
    JsonBooleanSchema(descriptionArb.bind())
}

private val leafSchemaArb = Arb.choice(stringSchemaArb, integerSchemaArb, booleanSchemaArb)

private fun schemaArb(depth: Int): Arb<JsonSchema> {
    if (depth == 0) return leafSchemaArb

    val child = schemaArb(depth - 1)
    val arraySchemaArb: Arb<JsonSchema> = arbitrary {
        JsonArraySchema(items = child.bind(), description = descriptionArb.bind())
    }
    val objectSchemaArb: Arb<JsonSchema> = arbitrary {
        val properties = Arb.map(nameArb, child, minSize = 0, maxSize = MAX_PROPERTIES).bind()
        JsonObjectSchema(
            properties = properties,
            required = properties.keys.filter { Arb.boolean().bind() }.toSet(),
            description = descriptionArb.bind(),
        )
    }
    return Arb.choice(leafSchemaArb, arraySchemaArb, objectSchemaArb)
}

private val declaredPropertyArb: Arb<DeclaredProperty> = arbitrary {
    DeclaredProperty(
        name = nameArb.bind(),
        schema = Arb.choice(
            leafSchemaArb,
            arbitrary<JsonSchema> {
                JsonArraySchema(leafSchemaArb.bind(), descriptionArb.bind())
            },
        ).bind(),
        required = Arb.boolean().bind(),
    )
}

private const val PROPERTY_ITERATIONS = 300
private const val MAX_SCHEMA_DEPTH = 3
private const val MAX_PROPERTIES = 4
