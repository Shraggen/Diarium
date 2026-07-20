package com.shraggen.diarium.tool

fun tool(

    name: String,

    block: ToolBuilder.() -> Unit,

    ): ToolSpecification =
    ToolBuilder(name)
        .apply(block)
        .build()
