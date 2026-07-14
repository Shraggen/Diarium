package com.shraggen.diarium

import kotlinx.coroutines.CancellationException

internal val DiariumUiState.isBusy: Boolean
    get() = isProcessing ||
        voiceStatus is VoiceStatus.Recording ||
        voiceStatus is VoiceStatus.Transcribing

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
