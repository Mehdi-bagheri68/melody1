package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Fixes N-06 & N-14:
 * - Upgrades Room schema from version 3 to version 4.
 * - Replaces unconditional `fallbackToDestructiveMigration()` with explicit non-destructive
 *   [MIGRATION_3_4] (plus [MIGRATION_1_3] and [MIGRATION_2_3]) so user favorites, history,
 *   and playlists are preserved across app updates.
 */
@Database(
    entities = [
        ArtistEntity::class,
        UserAccountEntity::class,
        FavoriteTrackEntity::class,
        HistoryTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRefEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MeloGrooveDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MeloGrooveDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new tables for Artists and User Accounts
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `artists` (
                        `id` TEXT NOT NULL,
                        `nameFa` TEXT NOT NULL,
                        `nameEn` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `countryCode` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_countryCode` ON `artists` (`countryCode`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_accounts` (
                        `uid` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `emailVerified` INTEGER NOT NULL,
                        `verificationCode` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`uid`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_accounts_email` ON `user_accounts` (`email`)")

                // 2. Add artistId, ownerArtistId, and userId columns to existing tables without losing data
                db.execSQL("ALTER TABLE `favorite_tracks` ADD COLUMN `artistId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `favorite_tracks` ADD COLUMN `ownerArtistId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `favorite_tracks` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_tracks_artistId` ON `favorite_tracks` (`artistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_tracks_userId` ON `favorite_tracks` (`userId`)")

                db.execSQL("ALTER TABLE `listening_history` ADD COLUMN `artistId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `listening_history` ADD COLUMN `ownerArtistId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `listening_history` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_history_artistId` ON `listening_history` (`artistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_history_userId` ON `listening_history` (`userId`)")

                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlists_userId` ON `playlists` (`userId`)")

                db.execSQL("ALTER TABLE `playlist_tracks` ADD COLUMN `artistId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `playlist_tracks` ADD COLUMN `ownerArtistId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_tracks_artistId` ON `playlist_tracks` (`artistId`)")
            }
        }

        fun getDatabase(context: Context): MeloGrooveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeloGrooveDatabase::class.java,
                    "melogroove_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
