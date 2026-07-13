package com.shraggen.diarium

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException

internal class AndroidModelStore(
    private val application: Application,
) {

    fun newestModel(): File? =
        modelsDirectory()
            .listFiles { file ->
                file.extension.equals("gguf", ignoreCase = true)
            }
            ?.maxByOrNull(File::lastModified)

    fun importModel(uri: Uri): File {
        val target = targetFile(uri)
        val partialTarget = File(target.parentFile, "${target.name}.part")
        removeIncompleteImport(partialTarget)
        copySelectedModel(uri, partialTarget)
        replaceTarget(partialTarget, target)
        return target
    }

    private fun targetFile(uri: Uri): File {
        val displayName = modelDisplayName(uri)
        require(displayName.endsWith(".gguf", ignoreCase = true)) {
            "Select a .gguf model file."
        }

        return File(availableModelsDirectory(), sanitizeFileName(displayName))
    }

    private fun availableModelsDirectory(): File {
        val directory = modelsDirectory()
        check(directory.exists() || directory.mkdirs()) {
            "Could not create the model directory."
        }
        return directory
    }

    private fun removeIncompleteImport(partialTarget: File) {
        if (partialTarget.exists() && !partialTarget.delete()) {
            throw IOException("Could not replace an incomplete model import.")
        }
    }

    private fun copySelectedModel(
        uri: Uri,
        partialTarget: File,
    ) {
        val input = checkNotNull(application.contentResolver.openInputStream(uri)) {
            "Could not open the selected model."
        }

        try {
            input.buffered().use { source ->
                partialTarget.outputStream().buffered().use { destination ->
                    source.copyTo(destination)
                }
            }
        } catch (exception: IOException) {
            partialTarget.delete()
            throw exception
        }
    }

    private fun replaceTarget(
        partialTarget: File,
        target: File,
    ) {
        if (target.exists() && !target.delete()) {
            throw IOException("Could not replace the previously imported model.")
        }

        if (!partialTarget.renameTo(target)) {
            partialTarget.copyTo(target, overwrite = true)
            partialTarget.delete()
        }
    }

    private fun modelDisplayName(uri: Uri): String =
        application.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        } ?: uri.lastPathSegment ?: "development-model.gguf"

    private fun sanitizeFileName(value: String): String =
        value
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(UNSAFE_FILE_NAME_CHARACTER, "_")

    private fun modelsDirectory(): File =
        File(application.filesDir, "models")

    private companion object {
        val UNSAFE_FILE_NAME_CHARACTER = Regex("[^A-Za-z0-9._-]")
    }
}
