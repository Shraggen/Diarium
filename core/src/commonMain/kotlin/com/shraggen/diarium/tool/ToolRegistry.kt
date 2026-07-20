package com.shraggen.diarium.tool

class ToolRegistry(

    tools: List<Tool>,
) {

    private val tools =
        tools.associateBy {
            it.specification.name
        }

    operator fun get(
        name: String,
    ): Tool? =
        tools[name]

    fun all(): List<Tool> =
        tools.values.toList()
}
