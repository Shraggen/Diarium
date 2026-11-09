package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.DiariumKernel
import com.shraggen.diarium.provider.StructuredJsonGenerator
import com.shraggen.diarium.tool.ToolCall
import com.shraggen.diarium.tool.ToolCallPlanner
import com.shraggen.diarium.tool.ToolResult
import kotlinx.serialization.json.JsonObject

/** What the journal does with one voice note once a tool call was proposed. */
sealed interface JournalOutcome {

    /** The model output is not a tool call, so nothing is proposed. */
    data object Malformed : JournalOutcome

    /** A proposal is shown, but confirmation is blocked and nothing can be recorded. */
    data class Blocked(
        val hive: FieldCheck,
        val queen: FieldCheck,
    ) : JournalOutcome

    /** Confirmation is offered; confirming records exactly these fields. */
    data class Recordable(
        val hiveId: String,
        val queenSeen: Boolean,
    ) : JournalOutcome
}

/** How one proposed field compares with the transcript. */
enum class FieldCheck {
    VERIFIED,
    MISMATCH,
    CANNOT_VERIFY,
}

data class JournalProposal(
    val call: ToolCall?,
    val outcome: JournalOutcome,
)

/**
 * The production voice-to-journal harness around one planner: kernel planning,
 * tool-call parsing, transcript consistency checks, and confirmation, backed by
 * an in-memory inspection repository. Mirrors the confirmation rule of the
 * shared UI's pending tool call.
 */
class VoiceNoteJournal private constructor(
    kernelFor: (RecordInspectionTool) -> DiariumKernel,
) {
    private val repository = InMemoryInspectionRepository()
    private val kernel = kernelFor(RecordInspectionTool(repository))

    suspend fun propose(transcript: String): JournalProposal {
        val call = try {
            kernel.plan(transcript)
        } catch (_: IllegalArgumentException) {
            return JournalProposal(call = null, outcome = JournalOutcome.Malformed)
        }
        return JournalProposal(call = call, outcome = outcomeOf(transcript, call))
    }

    /** Confirms the proposal when the journal offers confirmation; otherwise does nothing. */
    suspend fun confirm(proposal: JournalProposal): ToolResult? =
        proposal.call
            ?.takeIf { proposal.outcome is JournalOutcome.Recordable }
            ?.let { kernel.execute(it) }

    fun recorded(): List<InspectionRecord> = repository.records.toList()

    companion object {
        fun withModel(generator: StructuredJsonGenerator): VoiceNoteJournal =
            VoiceNoteJournal { tool -> DiariumKernel(listOf(tool), generator) }

        fun withPlanner(planner: ToolCallPlanner): VoiceNoteJournal =
            VoiceNoteJournal { tool -> DiariumKernel(listOf(tool), planner) }

        /** Replays a fixed model output instead of running a model. */
        fun replaying(modelOutput: String): VoiceNoteJournal =
            withModel(ReplayedModelOutput(modelOutput))
    }
}

private fun outcomeOf(
    transcript: String,
    call: ToolCall,
): JournalOutcome {
    val hive = HiveIdentifierConsistencyChecker().check(transcript, call)
    val queen = QueenObservationConsistencyChecker().check(transcript, call)
    return if (
        hive is HiveIdentifierConsistency.Verified &&
        queen is QueenObservationConsistency.Verified
    ) {
        JournalOutcome.Recordable(hive.proposedIdentifier, queen.proposedQueenSeen)
    } else {
        JournalOutcome.Blocked(hive.fieldCheck(), queen.fieldCheck())
    }
}

private fun HiveIdentifierConsistency.fieldCheck(): FieldCheck =
    when (this) {
        is HiveIdentifierConsistency.Verified -> FieldCheck.VERIFIED
        is HiveIdentifierConsistency.Mismatch -> FieldCheck.MISMATCH
        is HiveIdentifierConsistency.CannotVerify -> FieldCheck.CANNOT_VERIFY
    }

private fun QueenObservationConsistency.fieldCheck(): FieldCheck =
    when (this) {
        is QueenObservationConsistency.Verified -> FieldCheck.VERIFIED
        is QueenObservationConsistency.Mismatch -> FieldCheck.MISMATCH
        is QueenObservationConsistency.CannotVerify -> FieldCheck.CANNOT_VERIFY
    }

private class ReplayedModelOutput(
    private val output: String,
) : StructuredJsonGenerator {
    override suspend fun generate(prompt: String, schema: JsonObject): String = output
}

private class InMemoryInspectionRepository : InspectionRepository {
    val records = mutableListOf<InspectionRecord>()

    override suspend fun record(draft: InspectionDraft): InspectionRecord {
        val record = InspectionRecord(
            id = records.size.toLong() + 1,
            hiveId = draft.hiveId,
            queenSeen = draft.queenSeen,
            recordedAtEpochMillis = 1_000,
        )
        records += record
        return record
    }

    override suspend fun recent(limit: Int): List<InspectionRecord> =
        records.takeLast(limit).reversed()
}
