package com.simplenotes.di

import com.simplenotes.data.repository.ChallengeRepositoryImpl
import com.simplenotes.data.repository.GameRepositoryImpl
import com.simplenotes.data.repository.PreferencesRepositoryImpl
import com.simplenotes.data.repository.ProgressionRepositoryImpl
import com.simplenotes.domain.repository.ChallengeRepository
import com.simplenotes.domain.repository.GameRepository
import com.simplenotes.domain.repository.PreferencesRepository
import com.simplenotes.domain.repository.ProgressionRepository
import com.simplenotes.data.repository.NoteRepositoryImpl
import com.simplenotes.data.repository.TodoRepositoryImpl
import com.simplenotes.domain.repository.NoteRepository
import com.simplenotes.domain.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository
    @Binds @Singleton abstract fun bindChallengeRepository(impl: ChallengeRepositoryImpl): ChallengeRepository
    @Binds @Singleton abstract fun bindProgressionRepository(impl: ProgressionRepositoryImpl): ProgressionRepository
    @Binds @Singleton abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
    @Binds @Singleton abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
    @Binds @Singleton abstract fun bindTodoRepository(impl: TodoRepositoryImpl): TodoRepository
}
