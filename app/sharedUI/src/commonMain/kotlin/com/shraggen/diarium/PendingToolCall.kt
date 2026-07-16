package com.shraggen.diarium

import com.shraggen.diarium.beekeeping.HiveIdentifierConsistency
import com.shraggen.diarium.beekeeping.HiveIdentifierConsistencyChecker
import com.shraggen.diarium.beekeeping.QueenObservationConsistency
import com.shraggen.diarium.beekeeping.QueenObservationConsistencyChecker
import com.shraggen.diarium.tool.ToolCall

data class PendingToolCall(
    val call: ToolCall,
    val identifierConsistency: HiveIdentifierConsistency,
    val queenConsistency: QueenObservationConsistency,
) {
    val toolName: String
        get() = call.toolName

    val arguments: String
        get() = call.arguments.toString()

    val hiveId: String?
        get() = identifierConsistency.proposedIdentifier

    val queenSeen: Boolean?
        get() = queenConsistency.proposedQueenSeen

    val confirmationAllowed: Boolean
        get() = identifierConsistency is HiveIdentifierConsistency.Verified &&
            queenConsistency is QueenObservationConsistency.Verified
}

fun pendingToolCall(
    transcript: String,
    call: ToolCall,
    identifierChecker: HiveIdentifierConsistencyChecker =
        HiveIdentifierConsistencyChecker(),
    queenChecker: QueenObservationConsistencyChecker =
        QueenObservationConsistencyChecker(),
): PendingToolCall = PendingToolCall(
    call = call,
    identifierConsistency = identifierChecker.check(transcript, call),
    queenConsistency = queenChecker.check(transcript, call),
)
