package com.shraggen.diarium.persistence

import com.shraggen.diarium.beekeeping.InspectionDraft
import com.shraggen.diarium.beekeeping.InspectionRecord
import com.shraggen.diarium.beekeeping.InspectionRepository

class RoomInspectionRepository(
    private val inspectionDao: InspectionDao,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : InspectionRepository {

    override suspend fun record(draft: InspectionDraft): InspectionRecord {
        val hiveId = draft.hiveId.trim()
        require(hiveId.isNotEmpty()) {
            "Hive identifier must not be blank."
        }

        val entity = InspectionEntity(
            hiveId = hiveId,
            queenSeen = draft.queenSeen,
            recordedAtEpochMillis = currentTimeMillis(),
        )
        val id = inspectionDao.insert(entity)
        return entity.copy(id = id).toRecord()
    }

    override suspend fun recent(limit: Int): List<InspectionRecord> {
        require(limit > 0) {
            "Inspection limit must be positive."
        }
        return inspectionDao.recent(limit).map(InspectionEntity::toRecord)
    }
}

private fun InspectionEntity.toRecord() = InspectionRecord(
    id = id,
    hiveId = hiveId,
    queenSeen = queenSeen,
    recordedAtEpochMillis = recordedAtEpochMillis,
)
