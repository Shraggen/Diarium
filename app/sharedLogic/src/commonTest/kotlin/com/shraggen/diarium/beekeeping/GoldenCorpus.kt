package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.speech.SpeechLanguage

/**
 * The inspection a careful beekeeper expects the journal to make confirmable
 * from a voice note. Ground truth for both specs and model evaluation.
 */
sealed interface ExpectedInspection {

    fun isMetBy(outcome: JournalOutcome): Boolean

    /** Confirmation is offered with exactly these fields. */
    data class Recorded(
        val hiveId: String,
        val queenSeen: Boolean,
    ) : ExpectedInspection {
        override fun isMetBy(outcome: JournalOutcome): Boolean =
            outcome == JournalOutcome.Recordable(hiveId, queenSeen)
    }

    /**
     * The voice note is missing, contradicting, or hedging a field, so the
     * journal must not offer confirmation of any inspection.
     */
    data object Abstention : ExpectedInspection {
        override fun isMetBy(outcome: JournalOutcome): Boolean =
            outcome !is JournalOutcome.Recordable
    }
}

/**
 * One golden voice note.
 *
 * [transcript] is the reference transcript of what was said in [language],
 * and [expectedInspection] is its ground truth. [scriptedModelOutput] is a
 * fixed command-model response (from a field run or written by hand) whose
 * harness result must be [expectedOutcome]. A consented recording of the
 * note, if one exists, is named [audioClipName].
 */
data class GoldenVoiceNote(
    val name: String,
    val language: SpeechLanguage,
    val transcript: String,
    val expectedInspection: ExpectedInspection,
    val scriptedModelOutput: String,
    val expectedOutcome: JournalOutcome,
) {
    val audioClipName: String
        get() = name.lowercase().split(NON_WORD).filter { it.isNotEmpty() }.joinToString("-") + ".wav"

    private companion object {
        val NON_WORD = Regex("[^a-z0-9]+")
    }
}

private fun recorded(hiveId: String, queenSeen: Boolean) =
    ExpectedInspection.Recorded(hiveId, queenSeen)

private fun recordable(hiveId: String, queenSeen: Boolean) =
    JournalOutcome.Recordable(hiveId, queenSeen)

private fun blocked(hive: FieldCheck, queen: FieldCheck) =
    JournalOutcome.Blocked(hive, queen)

private fun recordInspectionOutput(hiveId: String, queenSeen: Boolean): String =
    """{"tool":"record_inspection","arguments":{"hive_id":"$hiveId","queen_seen":$queenSeen}}"""

val goldenCorpus: List<GoldenVoiceNote> = listOf(
    GoldenVoiceNote(
        name = "English valid command",
        language = SpeechLanguage.ENGLISH,
        transcript = "I inspected hive 4 and saw the queen.",
        expectedInspection = recorded("4", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("4", queenSeen = true),
        expectedOutcome = recordable("4", queenSeen = true),
    ),
    GoldenVoiceNote(
        name = "German number word with queen not seen",
        language = SpeechLanguage.GERMAN,
        transcript =
            "Bienenstock Nummer vier wurde kontrolliert und die Königin nicht gesehen.",
        expectedInspection = recorded("4", queenSeen = false),
        scriptedModelOutput = recordInspectionOutput("4", queenSeen = false),
        expectedOutcome = recordable("4", queenSeen = false),
    ),
    GoldenVoiceNote(
        name = "Noisy Serbian Latin command",
        language = SpeechLanguage.SERBIAN,
        transcript = "Hm... košnica, pet; matica je viđena.",
        expectedInspection = recorded("5", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("5", queenSeen = true),
        expectedOutcome = recordable("5", queenSeen = true),
    ),
    GoldenVoiceNote(
        name = "Serbian Cyrillic compound number words",
        language = SpeechLanguage.SERBIAN,
        transcript = "Прегледао сам кошницу четрдесет две и матица није виђена.",
        expectedInspection = recorded("42", queenSeen = false),
        scriptedModelOutput = recordInspectionOutput("42", queenSeen = false),
        expectedOutcome = recordable("42", queenSeen = false),
    ),
    GoldenVoiceNote(
        name = "Serbian Latin release acceptance phrase",
        language = SpeechLanguage.SERBIAN,
        transcript = "Pregledao sam košnicu 4 i video maticu.",
        expectedInspection = recorded("4", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("4", queenSeen = true),
        expectedOutcome = recordable("4", queenSeen = true),
    ),
    GoldenVoiceNote(
        name = "Serbian Cyrillic release acceptance phrase",
        language = SpeechLanguage.SERBIAN,
        transcript = "Прегледао сам кошницу 4 и видео матицу.",
        expectedInspection = recorded("4", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("4", queenSeen = true),
        expectedOutcome = recordable("4", queenSeen = true),
    ),
    GoldenVoiceNote(
        name = "Missing transcript identifier blocks an invented hive",
        language = SpeechLanguage.ENGLISH,
        transcript = "I inspected the hive and saw the queen.",
        expectedInspection = ExpectedInspection.Abstention,
        scriptedModelOutput = recordInspectionOutput("8", queenSeen = true),
        expectedOutcome = blocked(FieldCheck.CANNOT_VERIFY, FieldCheck.VERIFIED),
    ),
    GoldenVoiceNote(
        name = "Serbian transcript and proposal contradiction is blocked",
        language = SpeechLanguage.SERBIAN,
        transcript = "Pregledao sam košnicu, pet, i video maticu.",
        expectedInspection = recorded("5", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("4", queenSeen = true),
        expectedOutcome = blocked(FieldCheck.MISMATCH, FieldCheck.VERIFIED),
    ),
    GoldenVoiceNote(
        name = "Multiple explicit identifiers cannot be guessed",
        language = SpeechLanguage.ENGLISH,
        transcript = "I inspected hive 4, correction, hive 5, and saw the queen.",
        expectedInspection = ExpectedInspection.Abstention,
        scriptedModelOutput = recordInspectionOutput("5", queenSeen = true),
        expectedOutcome = blocked(FieldCheck.CANNOT_VERIFY, FieldCheck.VERIFIED),
    ),
    GoldenVoiceNote(
        name = "Malformed model JSON cannot produce a proposal",
        language = SpeechLanguage.ENGLISH,
        transcript = "I inspected hive 4.",
        expectedInspection = ExpectedInspection.Abstention,
        scriptedModelOutput = "not JSON",
        expectedOutcome = JournalOutcome.Malformed,
    ),
    GoldenVoiceNote(
        name = "Physical English run rejects field label inside identifier",
        language = SpeechLanguage.ENGLISH,
        transcript = "I inspected Hive 4 and saw the queen.",
        expectedInspection = recorded("4", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("Hive 4", queenSeen = true),
        expectedOutcome = blocked(FieldCheck.MISMATCH, FieldCheck.VERIFIED),
    ),
    GoldenVoiceNote(
        name = "Physical Serbian four run rejects hallucinated fields",
        language = SpeechLanguage.SERBIAN,
        transcript = "Прегледово, sam košnicu, četri, i video, maticu.",
        expectedInspection = recorded("4", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("trapeze_1", queenSeen = false),
        expectedOutcome = blocked(FieldCheck.MISMATCH, FieldCheck.MISMATCH),
    ),
    GoldenVoiceNote(
        name = "Physical German run rejects translated field label",
        language = SpeechLanguage.GERMAN,
        transcript = "Ich habe Bienenstock 4 kontrolliert und die Königin gesehen.",
        expectedInspection = recorded("4", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("Bienenstock 4", queenSeen = true),
        expectedOutcome = blocked(FieldCheck.MISMATCH, FieldCheck.VERIFIED),
    ),
    GoldenVoiceNote(
        name = "Physical German five run rejects omitted model identifier",
        language = SpeechLanguage.GERMAN,
        transcript = "Ich habe Bienenstock 5 kontrolliert und die Königin gesehen.",
        expectedInspection = recorded("5", queenSeen = true),
        scriptedModelOutput =
            """{"tool":"record_inspection","arguments":{"queen_seen":true}}""",
        expectedOutcome = blocked(FieldCheck.CANNOT_VERIFY, FieldCheck.VERIFIED),
    ),
    GoldenVoiceNote(
        name = "Physical Serbian five run rejects inflected identifier",
        language = SpeechLanguage.SERBIAN,
        transcript = "Прегледово, sam košnicu, пет и видео, sam, maticu.",
        expectedInspection = recorded("5", queenSeen = true),
        scriptedModelOutput = recordInspectionOutput("peta", queenSeen = true),
        expectedOutcome = blocked(FieldCheck.MISMATCH, FieldCheck.VERIFIED),
    ),
)
