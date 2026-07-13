package com.shraggen.diarium.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [InspectionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DiariumDatabase : RoomDatabase() {

    abstract fun inspectionDao(): InspectionDao

    companion object {
        @Volatile
        private var instance: DiariumDatabase? = null

        fun getInstance(context: Context): DiariumDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DiariumDatabase::class.java,
                    "diarium.db",
                ).build().also { database ->
                    instance = database
                }
            }
    }
}
