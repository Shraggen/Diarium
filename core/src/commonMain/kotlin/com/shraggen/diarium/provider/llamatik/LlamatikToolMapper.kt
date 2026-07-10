package com.shraggen.diarium.provider.llamatik

import com.shraggen.diarium.schema.toJsonObject
import com.shraggen.diarium.tool.Tool
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LlamatikToolMapper {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = false
    }

    fun schemaFor(tool: Tool): String =
        json.encodeToString(
            tool.specification.parameters.toJsonObject(),
        )
}
