package com.shraggen.diarium.beekeeping

data class InspectionRecord(
    val id: Long,
    val hiveId: String,
    val queenSeen: Boolean,
    val recordedAtEpochMillis: Long,
)

data class InspectionDraft(
    val hiveId: String,
    val queenSeen: Boolean,
)
