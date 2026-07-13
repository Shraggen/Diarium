package com.shraggen.diarium

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shraggen.diarium.tool.ToolResult
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiariumViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val controller = DiariumController()
    private val modelStore = AndroidModelStore(application)

    private val mutableUiState = MutableStateFlow(DiariumUiState())
    val uiState: StateFlow<DiariumUiState> = mutableUiState.asStateFlow()

    init {
        restoreExistingModel()
    }

    fun updateUserInput(value: String) {
        mutableUiState.update { state ->
            state.copy(userInput = value)
        }
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
                withContext(Dispatchers.IO) {
                    controller.shutdown()
                }

                val modelFile = withContext(Dispatchers.IO) {
                    modelStore.importModel(uri)
                }

                initializeModelFile(modelFile)
            }.exceptionOrNull()?.let(::reportModelError)
        }
    }

    fun processInput() {
        val state = mutableUiState.value
        if (state.isProcessing ||
            state.modelStatus !is ModelStatus.Ready ||
            state.userInput.isBlank()
        ) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { current ->
                current.copy(
                    isProcessing = true,
                    output = "",
                )
            }

            val outcome = runCatching {
                val result = withContext(Dispatchers.IO) {
                    controller.process(mutableUiState.value.userInput)
                }

                result.displayText()
            }

            mutableUiState.update { current ->
                current.copy(
                    isProcessing = false,
                    output = outcome.fold(
                        onSuccess = { it },
                        onFailure = { exception ->
                            exception.failureMessage(
                                prefix = "Inference failed: ",
                                fallback = "Unknown error.",
                            )
                        },
                    ),
                )
            }
        }
    }

    override fun onCleared() {
        controller.shutdown()
        super.onCleared()
    }

    private fun restoreExistingModel() {
        val modelFile = modelStore.newestModel()
            ?: return

        viewModelScope.launch {
            runCatching {
                initializeModelFile(modelFile)
            }.exceptionOrNull()?.let(::reportModelError)
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

        val initialized = withContext(Dispatchers.IO) {
            controller.initialize(modelFile.absolutePath)
        }

        check(initialized) {
            "Llamatik could not initialize the selected model."
        }

        mutableUiState.update { state ->
            state.copy(
                modelStatus = ModelStatus.Ready(modelFile.name),
            )
        }
    }

    private fun reportModelError(exception: Throwable) {
        controller.shutdown()
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

    private fun ToolResult.displayText(): String =
        when (this) {
            is ToolResult.Success -> content.toString()
            is ToolResult.Failure -> "Tool execution failed: $message"
        }

    private fun Throwable.failureMessage(
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
}
