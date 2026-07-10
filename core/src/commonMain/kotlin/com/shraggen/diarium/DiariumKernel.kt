package com.shraggen.diarium

/**
 * The Voice OS Core. It orchestrates the flow but knows nothing about the domain.
 */
class DiariumKernel(private val registeredTools: List<DiariumTool>) {

    // This simulates what will eventually happen after the LLM parses the voice input.
    fun processSimulatedLlmCall(toolName: String, arguments: Map<String, String>): String {
        val tool = registeredTools.find { it.name == toolName }
            ?: return "Kernel Error: Tool '$toolName' is not registered."

        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            "Kernel Error: Tool execution failed - ${e.message}"
        }
    }

    fun getAvailableTools(): List<String> {
        return registeredTools.map { it.name }
    }
}