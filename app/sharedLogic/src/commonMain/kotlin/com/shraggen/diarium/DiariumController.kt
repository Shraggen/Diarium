package com.shraggen.diarium

import com.shraggen.diarium.provider.llamatik.LlamatikStructuredJsonGenerator
import com.shraggen.diarium.tool.ToolResult

class DiariumController(
    private val generator: LlamatikStructuredJsonGenerator =
        LlamatikStructuredJsonGenerator(),
) {

    private var kernel: DiariumKernel? = null
    private var isInitialized = false

    fun initialize(modelLocation: String): Boolean {
        shutdown()

        if (!generator.initialize(modelLocation)) {
            return false
        }

        isInitialized = true
        kernel = DiariumKernel(
            registeredTools = listOf(RecordInspectionTool()),
            generator = generator,
        )
        return true
    }

    suspend fun process(userInput: String): ToolResult =
        kernel?.process(userInput)
            ?: ToolResult.Failure(
                message = "The local model is not initialized.",
            )

    fun shutdown() {
        kernel = null

        if (isInitialized) {
            generator.shutdown()
            isInitialized = false
        }
    }
}
