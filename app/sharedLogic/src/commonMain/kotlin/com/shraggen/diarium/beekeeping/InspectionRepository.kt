package com.shraggen.diarium.beekeeping

interface InspectionRepository {

    suspend fun record(draft: InspectionDraft): InspectionRecord

    suspend fun recent(limit: Int = DEFAULT_RECENT_INSPECTIONS): List<InspectionRecord>
}

const val DEFAULT_RECENT_INSPECTIONS = 20
