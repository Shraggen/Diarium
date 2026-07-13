package com.shraggen.diarium

import com.shraggen.diarium.beekeeping.DEFAULT_RECENT_INSPECTIONS
import com.shraggen.diarium.beekeeping.InspectionRecord
import com.shraggen.diarium.beekeeping.InspectionRepository
import com.shraggen.diarium.beekeeping.RecordInspectionTool
import com.shraggen.diarium.provider.llamatik.LlamatikStructuredJsonGenerator
import com.shraggen.diarium.tool.ToolCall
import com.shraggen.diarium.tool.ToolResult

class DiariumController(
    private val inspectionRepository: InspectionRepository,
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
            registeredTools = listOf(RecordInspectionTool(inspectionRepository)),
            generator = generator,
        )
        return true
    }

    suspend fun process(userInput: String): ToolResult =
        kernel?.process(userInput)
            ?: ToolResult.Failure(
                message = "The local model is not initialized.",
            )

    suspend fun plan(userInput: String): ToolCall =
        requireNotNull(kernel) {
            "The local model is not initialized."
        }.plan(userInput)

    suspend fun execute(call: ToolCall): ToolResult =
        kernel?.execute(call)
            ?: ToolResult.Failure(
                message = "The local model is not initialized.",
            )

    suspend fun recentInspections(
        limit: Int = DEFAULT_RECENT_INSPECTIONS,
    ): List<InspectionRecord> = inspectionRepository.recent(limit)

    fun shutdown() {
        kernel = null

        if (isInitialized) {
            generator.shutdown()
            isInitialized = false
        }
    }
}
