package com.shraggen.diarium.tool

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.jvm.JvmInline

@JvmInline
value class ToolArguments(

    val json: JsonObject,
) {

    fun string(name: String): String =
        json[name]!!.jsonPrimitive.content

    fun stringOrNull(name: String): String? =
        json[name]?.jsonPrimitive?.content

    fun int(name: String): Int =
        json[name]!!.jsonPrimitive.int

    fun intOrNull(name: String): Int? =
        json[name]?.jsonPrimitive?.int

    fun boolean(name: String): Boolean =
        json[name]!!.jsonPrimitive.boolean

    fun booleanOrNull(name: String): Boolean? {
        val primitive = json[name] as? JsonPrimitive ?: return null
        return primitive.takeUnless(JsonPrimitive::isString)?.booleanOrNull
    }
}
