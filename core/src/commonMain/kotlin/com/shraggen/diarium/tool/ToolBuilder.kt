package com.shraggen.diarium.tool

import com.shraggen.diarium.schema.JsonObjectBuilder
import com.shraggen.diarium.schema.obj

class ToolBuilder(

    private val name: String,
) {

    private var description = ""

    private var schema =
        obj { }

    fun description(
        value: String,
    ) {
        description = value
    }

    fun parameters(
        block: JsonObjectBuilder.() -> Unit,
    ) {

        schema =
            obj(block)
    }

    internal fun build() =
        ToolSpecification(

            name,

            description,

            schema,
        )
}
