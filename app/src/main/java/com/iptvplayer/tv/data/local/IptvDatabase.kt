package com.iptvplayer.tv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistContentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
