package com.shraggen.diarium

import android.net.Uri
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LlmModelCoordinator(
    private val runtime: AndroidDiariumRuntime,
    private val mutableUiState: MutableStateFlow<DiariumUiState>,
    private val coroutineScope: CoroutineScope,
) {

    fun load(uri: Uri) {
        if (mutableUiState.value.modelStatus is ModelStatus.Loading ||
            mutableUiState.value.isBusy
        ) {
            return
        }

        coroutineScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    modelStatus = ModelStatus.Loading("Copying model to app storage…"),
                    output = "",
                )
            }

            runCatching {
                initialize(runtime.importLlmModel(uri))
            }.exceptionOrNull()?.let(::reportError)
        }
    }

    fun restore() {
        val modelFile = runtime.newestLlmModel() ?: return
        coroutineScope.launch {
            runCatching {
                initialize(modelFile)
            }.exceptionOrNull()?.let(::reportError)
        }
    }

    private suspend fun initialize(modelFile: File) {
        mutableUiState.update { state ->
            state.copy(
                modelStatus = ModelStatus.Loading("Loading ${modelFile.name}…"),
            )
        }
        runtime.initializeLlm(modelFile)
        mutableUiState.update { state ->
            state.copy(modelStatus = ModelStatus.Ready(modelFile.name))
        }
    }

    private fun reportError(exception: Throwable) {
        mutableUiState.update { state ->
            state.copy(
                modelStatus = ModelStatus.Error(
                    exception.failureMessage(
                        prefix = "",
                        fallback = "Model initialization failed.",
                    ),
                ),
            )
        }
    }
}
