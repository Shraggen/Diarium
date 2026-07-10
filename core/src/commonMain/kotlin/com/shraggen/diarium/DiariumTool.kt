package com.shraggen.diarium

/**
 * The fundamental contract for any Domain Plugin in Diarium.
 */
interface DiariumTool {
    val name: String
    val description: String

    // In the future, arguments will be parsed from the LLM's JSON output.
    fun execute(arguments: Map<String, String>): String
}