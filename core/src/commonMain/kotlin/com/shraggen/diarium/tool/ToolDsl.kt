package com.shraggen.diarium.tool

import com.shraggen.diarium.schema.*

fun tool(

    name: String,

    block: ToolBuilder.() -> Unit,

    ): ToolSpecification =
    ToolBuilder(name)
        .apply(block)
        .build()
