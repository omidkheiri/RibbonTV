package com.iptvplayer.tv.di

import com.iptvplayer.tv.data.remote.DefaultM3uParser
import com.iptvplayer.tv.data.remote.M3uParser
import com.iptvplayer.tv.data.repository.DefaultPlaylistRepository
import com.iptvplayer.tv.domain.classification.ContentClassifier
import com.iptvplayer.tv.domain.classification.DefaultContentClassifier
import com.iptvplayer.tv.domain.repository.PlaylistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds
    abstract fun bindM3uParser(parser: DefaultM3uParser): M3uParser

    @Binds
    abstract fun bindContentClassifier(classifier: DefaultContentClassifier): ContentClassifier

    @Binds
    abstract fun bindPlaylistRepository(repository: DefaultPlaylistRepository): PlaylistRepository
}
