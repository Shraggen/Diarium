package com.shraggen.diarium.schema

import kotlinx.serialization.json.JsonObject

fun JsonSchema.toJsonObject(): JsonObject =
    JsonSchemaSerializer.serialize(this)
