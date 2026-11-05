package com.iptvplayer.tv.di

import android.content.Context
import androidx.room.Room
import com.iptvplayer.tv.data.local.IptvDatabase
import com.iptvplayer.tv.data.local.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideIptvDatabase(@ApplicationContext context: Context): IptvDatabase =
        Room.databaseBuilder(context, IptvDatabase::class.java, "iptv-player.db").build()

    @Provides
    fun providePlaylistDao(database: IptvDatabase): PlaylistDao = database.playlistDao()
}
