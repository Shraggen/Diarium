package com.shraggen.diarium.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "hive_id")
    val hiveId: String,
    @ColumnInfo(name = "queen_seen")
    val queenSeen: Boolean,
    @ColumnInfo(name = "recorded_at_epoch_millis")
    val recordedAtEpochMillis: Long,
)
