package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.tool.ToolCall
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

const val QUEEN_SEEN_ARGUMENT_NAME = "queen_seen"

sealed interface QueenObservationConsistency {
    val proposedQueenSeen: Boolean?

    data class Verified(
        val transcriptQueenSeen: Boolean,
        override val proposedQueenSeen: Boolean,
    ) : QueenObservationConsistency

    data class Mismatch(
        val transcriptQueenSeen: Boolean,
        override val proposedQueenSeen: Boolean,
    ) : QueenObservationConsistency

    data class CannotVerify(
        val transcriptObservations: Set<Boolean>,
        override val proposedQueenSeen: Boolean?,
    ) : QueenObservationConsistency
}

class QueenObservationExtractor {

    fun extract(transcript: String): Set<Boolean> =
        extractQueenObservations(transcript)
}

class QueenObservationConsistencyChecker(
    private val extractor: QueenObservationExtractor = QueenObservationExtractor(),
) {

    fun check(
        transcript: String,
        call: ToolCall,
    ): QueenObservationConsistency {
        val proposedQueenSeen = call.proposedQueenSeen()
        val transcriptObservations = extractor.extract(transcript)

        if (call.toolName != RECORD_INSPECTION_TOOL_NAME ||
            proposedQueenSeen == null ||
            transcriptObservations.size != 1
        ) {
            return QueenObservationConsistency.CannotVerify(
                transcriptObservations = transcriptObservations,
                proposedQueenSeen = proposedQueenSeen,
            )
        }

        val transcriptQueenSeen = transcriptObservations.single()
        return if (transcriptQueenSeen == proposedQueenSeen) {
            QueenObservationConsistency.Verified(
                transcriptQueenSeen = transcriptQueenSeen,
                proposedQueenSeen = proposedQueenSeen,
            )
        } else {
            QueenObservationConsistency.Mismatch(
                transcriptQueenSeen = transcriptQueenSeen,
                proposedQueenSeen = proposedQueenSeen,
            )
        }
    }
}

private fun ToolCall.proposedQueenSeen(): Boolean? =
    (arguments[QUEEN_SEEN_ARGUMENT_NAME] as? JsonPrimitive)
        ?.takeUnless(JsonPrimitive::isString)
        ?.booleanOrNull

private fun extractQueenObservations(transcript: String): Set<Boolean> {
    val tokens = tokenizeQueenTranscript(transcript)
    val negativeSeen = NEGATIVE_QUEEN_PHRASES.any(tokens::containsUnqualifiedPhrase)
    val positiveSeen = POSITIVE_QUEEN_PHRASES.any { phrase ->
        tokens.containsUnqualifiedPhrase(phrase)
    }

    return buildSet {
        if (negativeSeen) add(false)
        if (positiveSeen) add(true)
        addAll(tokens.serbianQueenObservations())
    }
}

private fun List<String>.serbianQueenObservations(): Set<Boolean> = buildSet {
    this@serbianQueenObservations.indices
        .filter { this@serbianQueenObservations[it] in SERBIAN_SEE_WORDS }
        .filter { verbIndex ->
            this@serbianQueenObservations.indices.any { queenIndex ->
                this@serbianQueenObservations[queenIndex] in SERBIAN_QUEEN_WORDS &&
                    kotlin.math.abs(queenIndex - verbIndex) <= SERBIAN_PHRASE_WINDOW_SIZE
            }
        }
        .forEach { verbIndex ->
            if (!this@serbianQueenObservations.hasPrecedingUncertainty(verbIndex)) {
                add(!this@serbianQueenObservations.hasPrecedingNegation(verbIndex))
            }
        }
}

private fun tokenizeQueenTranscript(value: String): List<String> =
    value
        .lowercase()
        .map { character ->
            if (character.isLetterOrDigit()) character else ' '
        }
        .joinToString(separator = "")
        .split(' ')
        .filter(String::isNotBlank)

private fun List<String>.containsUnqualifiedPhrase(
    phrase: List<String>,
): Boolean = indices.any { startIndex ->
    matchesAt(startIndex, phrase) &&
        !hasPrecedingNegation(startIndex) &&
        !hasPrecedingUncertainty(startIndex)
}

private fun List<String>.matchesAt(
    startIndex: Int,
    phrase: List<String>,
): Boolean = subList(
    fromIndex = startIndex,
    toIndex = minOf(size, startIndex + phrase.size),
) == phrase

private fun List<String>.hasPrecedingNegation(startIndex: Int): Boolean =
    subList(
        fromIndex = maxOf(0, startIndex - NEGATION_WINDOW_SIZE),
        toIndex = startIndex,
    ).any(NEGATION_WORDS::contains)

private fun List<String>.hasPrecedingUncertainty(startIndex: Int): Boolean =
    subList(
        fromIndex = maxOf(0, startIndex - NEGATION_WINDOW_SIZE),
        toIndex = startIndex,
    ).any(UNCERTAINTY_WORDS::contains)

private const val NEGATION_WINDOW_SIZE = 3
private const val SERBIAN_PHRASE_WINDOW_SIZE = 4

private val NEGATION_WORDS = setOf(
    "not",
    "no",
    "didn",
    "couldn",
    "wasn",
    "nicht",
    "kein",
    "keine",
    "nisam",
    "nismo",
    "nije",
    "нисам",
    "нисмо",
    "није",
)

private val UNCERTAINTY_WORDS = setOf(
    "maybe",
    "perhaps",
    "possibly",
    "vielleicht",
    "möglicherweise",
    "možda",
    "mozda",
    "можда",
)

private val POSITIVE_QUEEN_PHRASES = listOf(
    listOf("saw", "the", "queen"),
    listOf("saw", "queen"),
    listOf("see", "the", "queen"),
    listOf("see", "queen"),
    listOf("queen", "was", "seen"),
    listOf("queen", "seen"),
    listOf("observed", "the", "queen"),
    listOf("found", "the", "queen"),
    listOf("die", "königin", "gesehen"),
    listOf("königin", "gesehen"),
    listOf("die", "koenigin", "gesehen"),
    listOf("koenigin", "gesehen"),
    listOf("königin", "gefunden"),
    listOf("koenigin", "gefunden"),
)

private val NEGATIVE_QUEEN_PHRASES = listOf(
    listOf("did", "not", "see", "the", "queen"),
    listOf("did", "not", "see", "queen"),
    listOf("didn", "t", "see", "the", "queen"),
    listOf("didn", "t", "see", "queen"),
    listOf("queen", "was", "not", "seen"),
    listOf("queen", "not", "seen"),
    listOf("no", "queen", "seen"),
    listOf("königin", "nicht", "gesehen"),
    listOf("koenigin", "nicht", "gesehen"),
    listOf("keine", "königin", "gesehen"),
    listOf("keine", "koenigin", "gesehen"),
    listOf("königin", "nicht", "gefunden"),
)

private val SERBIAN_SEE_WORDS = setOf(
    "video",
    "videla",
    "videli",
    "vidio",
    "viđena",
    "vidjena",
    "видео",
    "видела",
    "видели",
    "виђена",
)

private val SERBIAN_QUEEN_WORDS = setOf(
    "matica",
    "maticu",
    "matici",
    "матица",
    "матицу",
    "матици",
)
