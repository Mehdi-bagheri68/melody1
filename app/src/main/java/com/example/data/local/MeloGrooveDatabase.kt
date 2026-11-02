package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteTrackEntity::class,
        HistoryTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRefEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MeloGrooveDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MeloGrooveDatabase? = null

        fun getDatabase(context: Context): MeloGrooveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeloGrooveDatabase::class.java,
                    "melogroove_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
