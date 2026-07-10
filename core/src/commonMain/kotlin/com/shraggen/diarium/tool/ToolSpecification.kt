package com.shraggen.diarium.tool

import com.shraggen.diarium.schema.JsonObjectSchema

data class ToolSpecification(

    val name: String,

    val description: String,

    val parameters: JsonObjectSchema,
)
