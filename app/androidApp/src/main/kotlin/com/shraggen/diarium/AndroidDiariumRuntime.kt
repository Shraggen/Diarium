package com.shraggen.diarium

import android.app.Application
import android.net.Uri
import com.shraggen.diarium.beekeeping.InspectionRecord
import com.shraggen.diarium.persistence.DiariumDatabase
import com.shraggen.diarium.persistence.RoomInspectionRepository
import com.shraggen.diarium.tool.ToolResult
import com.shraggen.diarium.tool.ToolCall
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDiariumRuntime(
    application: Application,
) {

    private val inspectionRepository = RoomInspectionRepository(
        DiariumDatabase.getInstance(application).inspectionDao(),
    )
    private val controller = DiariumController(inspectionRepository)
    private val modelStore = AndroidModelStore(application)

    fun newestLlmModel(): File? = modelStore.newestModel(StoredModelType.LLM)

    suspend fun importLlmModel(uri: Uri): File = withContext(Dispatchers.IO) {
        controller.shutdown()
        modelStore.importModel(uri, StoredModelType.LLM)
    }

    suspend fun initializeLlm(modelFile: File) = withContext(Dispatchers.IO) {
        check(controller.initialize(modelFile.absolutePath)) {
            "Llamatik could not initialize the selected model."
        }
    }

    suspend fun plan(userInput: String): ToolCall =
        withContext(Dispatchers.IO) {
            controller.plan(userInput)
        }

    suspend fun execute(call: ToolCall): RuntimeProcessingOutcome =
        withContext(Dispatchers.IO) {
            RuntimeProcessingOutcome(
                result = controller.execute(call),
                inspections = controller.recentInspections(),
            )
        }

    suspend fun recentInspections(): List<InspectionRecord> =
        withContext(Dispatchers.IO) {
            controller.recentInspections()
        }

    fun shutdown() {
        controller.shutdown()
    }
}

data class RuntimeProcessingOutcome(
    val result: ToolResult,
    val inspections: List<InspectionRecord>,
)
