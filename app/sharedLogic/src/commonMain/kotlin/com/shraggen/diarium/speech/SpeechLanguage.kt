package com.shraggen.diarium.speech

enum class SpeechLanguage(
    val whisperCode: String,
    val displayName: String,
    val whisperPrompt: String,
) {
    ENGLISH(
        whisperCode = "en",
        displayName = "English",
        whisperPrompt = "Beehive inspection. Hive, queen, apiary.",
    ),
    GERMAN(
        whisperCode = "de",
        displayName = "Deutsch",
        whisperPrompt = "Bienenstockinspektion. Bienenstock, Königin, Bienenstand.",
    ),
    SERBIAN(
        whisperCode = "sr",
        displayName = "Srpski / Српски",
        whisperPrompt =
            "Pregled košnice. Košnica, matica, pčelinjak. " +
                "Преглед кошнице. Кошница, матица, пчелињак.",
    ),
    ;

    companion object {
        fun fromLanguageTag(languageTag: String): SpeechLanguage =
            when (languageTag.substringBefore('-').substringBefore('_').lowercase()) {
                GERMAN.whisperCode -> GERMAN
                SERBIAN.whisperCode -> SERBIAN
                else -> ENGLISH
            }
    }
}
