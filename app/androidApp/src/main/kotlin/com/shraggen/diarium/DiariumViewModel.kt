package com.shraggen.diarium

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiariumViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val runtime = AndroidDiariumRuntime(application)

    private val mutableUiState = MutableStateFlow(DiariumUiState())
    val uiState: StateFlow<DiariumUiState> = mutableUiState.asStateFlow()
    private val toolCallCoordinator = ToolCallCoordinator(
        runtime = runtime,
        mutableUiState = mutableUiState,
        coroutineScope = viewModelScope,
    )

    init {
        refreshRecentInspections()
        restoreExistingModel()
    }

    fun updateUserInput(value: String) {
        toolCallCoordinator.updateUserInput(value)
    }

    fun loadModel(uri: Uri) {
        if (mutableUiState.value.modelStatus is ModelStatus.Loading ||
            mutableUiState.value.isProcessing
        ) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    modelStatus = ModelStatus.Loading("Copying model to app storage…"),
                    output = "",
                )
            }

            runCatching {
                val modelFile = runtime.importModel(uri)
                initializeModelFile(modelFile)
            }.exceptionOrNull()?.let(::reportModelError)
        }
    }

    fun processInput() {
        toolCallCoordinator.plan()
    }

    fun confirmToolCall() {
        toolCallCoordinator.confirm()
    }

    fun cancelToolCall() {
        toolCallCoordinator.cancel()
    }

    override fun onCleared() {
        runtime.shutdown()
        super.onCleared()
    }

    private fun restoreExistingModel() {
        val modelFile = runtime.newestModel()
            ?: return

        viewModelScope.launch {
            runCatching {
                initializeModelFile(modelFile)
            }.exceptionOrNull()?.let(::reportModelError)
        }
    }

    private fun refreshRecentInspections() {
        viewModelScope.launch {
            val outcome = runCatching {
                runtime.recentInspections()
            }

            mutableUiState.update { state ->
                state.copy(
                    recentInspections = outcome.getOrDefault(
                        state.recentInspections,
                    ),
                    journalError = outcome.exceptionOrNull()?.failureMessage(
                        prefix = "Could not load journal: ",
                        fallback = "Unknown database error.",
                    ),
                )
            }
        }
    }

    private suspend fun initializeModelFile(modelFile: File) {
        mutableUiState.update { state ->
            state.copy(
                modelStatus = ModelStatus.Loading(
                    "Loading ${modelFile.name}…",
                ),
            )
        }

        runtime.initialize(modelFile)

        mutableUiState.update { state ->
            state.copy(
                modelStatus = ModelStatus.Ready(modelFile.name),
            )
        }
    }

    private fun reportModelError(exception: Throwable) {
        runtime.shutdown()
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

internal fun Throwable.failureMessage(
    prefix: String,
    fallback: String,
): String {
    if (this is CancellationException) {
        throw this
    }
    if (this !is Exception) {
        throw this
    }
    return prefix + (message ?: fallback)
}
