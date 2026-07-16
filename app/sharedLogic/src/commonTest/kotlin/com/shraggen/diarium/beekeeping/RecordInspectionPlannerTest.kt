package com.shraggen.diarium.beekeeping

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RecordInspectionPlannerTest {

    private val planner = RecordInspectionPlanner()

    @Test
    fun fieldRecordingsProduceCanonicalDeterministicCalls() = runTest {
        val cases = listOf(
            FieldRecording(
                name = "English hive four",
                transcript = "I inspected Hive 4 and saw the queen.",
                expectedHiveId = "4",
            ),
            FieldRecording(
                name = "German hive five",
                transcript =
                    "Ich habe Bienenstock 5 kontrolliert und die Königin gesehen.",
                expectedHiveId = "5",
            ),
            FieldRecording(
                name = "German hive four",
                transcript =
                    "Ich habe Bienenstock 4 kontrolliert und die Königin gesehen.",
                expectedHiveId = "4",
            ),
            FieldRecording(
                name = "Mixed-script Serbian hive five",
                transcript =
                    "Прегледово, sam košnicu, пет и видео, sam, maticu.",
                expectedHiveId = "5",
            ),
        )

        cases.forEach { case ->
            val call = planner.plan(case.transcript)

            assertEquals(RECORD_INSPECTION_TOOL_NAME, call.toolName, case.name)
            assertEquals(
                case.expectedHiveId,
                call.arguments.getValue(HIVE_ID_ARGUMENT_NAME).jsonPrimitive.content,
                case.name,
            )
            assertEquals(
                true,
                call.arguments.getValue(QUEEN_SEEN_ARGUMENT_NAME).jsonPrimitive.boolean,
                case.name,
            )
        }
    }

    @Test
    fun ambiguousFieldsAreOmittedInsteadOfGuessed() = runTest {
        val call = planner.plan(
            "I inspected hive 4, correction, hive 5, and maybe saw the queen.",
        )

        assertFalse(call.arguments.containsKey(HIVE_ID_ARGUMENT_NAME))
        assertFalse(call.arguments.containsKey(QUEEN_SEEN_ARGUMENT_NAME))
    }
}

private data class FieldRecording(
    val name: String,
    val transcript: String,
    val expectedHiveId: String,
)
