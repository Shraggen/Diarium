package com.shraggen.diarium.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InspectionDao {

    @Insert
    suspend fun insert(inspection: InspectionEntity): Long

    @Query(
        """
        SELECT * FROM inspections
        ORDER BY recorded_at_epoch_millis DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun recent(limit: Int): List<InspectionEntity>
}
