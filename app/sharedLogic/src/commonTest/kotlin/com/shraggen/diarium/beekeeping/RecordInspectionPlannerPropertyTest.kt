package com.shraggen.diarium.beekeeping

import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordInspectionPlannerPropertyTest {

    private val planner = RecordInspectionPlanner()

    @Test
    fun numericIdentifiersRemainCanonicalAcrossLanguagesAndFormatting() = runTest {
        checkAll(
            iterations = PROPERTY_ITERATIONS,
            Arb.int(0..MAX_GENERATED_HIVE_ID),
            Arb.element(commandTemplates),
            Arb.element(tokenSeparators),
            Arb.element(listOf(false, true)),
        ) { hiveId, template, separator, uppercase ->
            val plainTranscript = template.tokens(hiveId.toString()).joinToString(separator)
            val transcript = plainTranscript
                .let { if (uppercase) it.uppercase() else it }
                .let { "  $it!!!  " }

            val firstCall = planner.plan(transcript)
            val repeatedCall = planner.plan(transcript)

            assertEquals(firstCall, repeatedCall)
            assertEquals(
                hiveId.toString(),
                firstCall.arguments
                    .getValue(HIVE_ID_ARGUMENT_NAME)
                    .jsonPrimitive
                    .content,
            )
            assertEquals(
                true,
                firstCall.arguments
                    .getValue(QUEEN_SEEN_ARGUMENT_NAME)
                    .jsonPrimitive
                    .boolean,
            )
        }
    }

    @Test
    fun multipleDifferentIdentifiersAreNeverCollapsedIntoOne() = runTest {
        checkAll(
            iterations = PROPERTY_ITERATIONS,
            Arb.int(0 until MAX_GENERATED_HIVE_ID),
            Arb.int(1..MAX_GENERATED_HIVE_ID),
        ) { firstHiveId, offset ->
            val secondHiveId = (firstHiveId + offset) % (MAX_GENERATED_HIVE_ID + 1)
            val call = planner.plan(
                "I inspected hive $firstHiveId, correction, " +
                    "hive $secondHiveId, and saw the queen.",
            )

            assertFalse(call.arguments.containsKey(HIVE_ID_ARGUMENT_NAME))
            assertEquals(
                true,
                call.arguments
                    .getValue(QUEEN_SEEN_ARGUMENT_NAME)
                    .jsonPrimitive
                    .boolean,
            )
        }
    }

    @Test
    fun unrelatedTextNeverInventsInspectionFacts() = runTest {
        val safeWord = Arb.element(unrelatedWords)

        checkAll(
            iterations = PROPERTY_ITERATIONS,
            safeWord,
            safeWord,
            safeWord,
            safeWord,
        ) { first, second, third, fourth ->
            val call = planner.plan("$first $second $third $fourth")

            assertTrue(call.arguments.isEmpty())
        }
    }
}

private data class CommandTemplate(
    val tokens: (String) -> List<String>,
)

private const val PROPERTY_ITERATIONS = 300
private const val MAX_GENERATED_HIVE_ID = 9_999

private val commandTemplates = listOf(
    CommandTemplate { hiveId ->
        listOf("I", "inspected", "hive", hiveId, "and", "saw", "the", "queen")
    },
    CommandTemplate { hiveId ->
        listOf(
            "Ich",
            "habe",
            "Bienenstock",
            hiveId,
            "kontrolliert",
            "und",
            "die",
            "Königin",
            "gesehen",
        )
    },
    CommandTemplate { hiveId ->
        listOf(
            "Pregledao",
            "sam",
            "košnicu",
            hiveId,
            "i",
            "video",
            "sam",
            "maticu",
        )
    },
    CommandTemplate { hiveId ->
        listOf(
            "Прегледао",
            "сам",
            "кошницу",
            hiveId,
            "и",
            "видео",
            "сам",
            "матицу",
        )
    },
)

private val tokenSeparators = listOf(
    " ",
    "  ",
    ", ",
    "\n",
    "\t",
)

private val unrelatedWords = listOf(
    "weather",
    "frames",
    "honey",
    "quiet",
    "Werkzeug",
    "sonnig",
    "pčele",
    "polen",
    "време",
    "мед",
)
