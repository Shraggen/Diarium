package com.shraggen.diarium

import com.shraggen.diarium.beekeeping.DEFAULT_RECENT_INSPECTIONS
import com.shraggen.diarium.beekeeping.InspectionRecord
import com.shraggen.diarium.beekeeping.InspectionRepository
import com.shraggen.diarium.beekeeping.RecordInspectionPlanner
import com.shraggen.diarium.beekeeping.RecordInspectionTool
import com.shraggen.diarium.tool.ToolCall
import com.shraggen.diarium.tool.ToolResult

class DiariumController(
    private val inspectionRepository: InspectionRepository,
) {

    private val kernel = DiariumKernel(
        registeredTools = listOf(RecordInspectionTool(inspectionRepository)),
        planner = RecordInspectionPlanner(),
    )
    suspend fun process(userInput: String): ToolResult =
        kernel.process(userInput)

    suspend fun plan(userInput: String): ToolCall =
        kernel.plan(userInput)

    suspend fun execute(call: ToolCall): ToolResult =
        kernel.execute(call)

    suspend fun recentInspections(
        limit: Int = DEFAULT_RECENT_INSPECTIONS,
    ): List<InspectionRecord> = inspectionRepository.recent(limit)

}
