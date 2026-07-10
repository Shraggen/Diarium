package com.shraggen.diarium.schema

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class JsonSchemaSerializerTest {

    @Test
    fun serializesSimpleObject() {

        val schema =
            obj {

                string("city")

                integer("days")

                required(
                    "city",
                    "days",
                )
            }

        val json =
            schema.toJsonObject()

        assertEquals(
            "object",
            json["type"]!!.jsonPrimitive.content,
        )

        val properties =
            json["properties"]!!.jsonObject

        assertTrue(properties.containsKey("city"))

        assertTrue(properties.containsKey("days"))
    }

    @Test
    fun serializesRequiredArray() {

        val schema =
            obj {

                string("city")

                required("city")
            }

        val required =
            schema
                .toJsonObject()["required"]!!
                .jsonArray

        assertEquals(
            "city",
            required.first().jsonPrimitive.content,
        )
    }

    @Test
    fun serializesEnums() {

        val schema =
            obj {

                string("unit") {
                    enum(
                        "metric",
                        "imperial",
                    )
                }
            }

        val property =
            schema
                .toJsonObject()["properties"]!!
                .jsonObject["unit"]!!
                .jsonObject

        val enum =
            property["enum"]!!
                .jsonArray

        assertEquals(
            2,
            enum.size,
        )
    }

    @Test
    fun serializesNestedObjects() {

        val schema =
            obj {

                objectProperty("location") {

                    string("city")

                    required("city")
                }
            }

        val location =
            schema
                .toJsonObject()["properties"]!!
                .jsonObject["location"]!!
                .jsonObject

        assertEquals(
            "object",
            location["type"]!!.jsonPrimitive.content,
        )
    }
}
