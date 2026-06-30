package com.simplenotes.di

import android.content.Context
import androidx.room.Room
import com.simplenotes.data.local.database.SimpleNotesDatabase
import com.simplenotes.data.local.database.dao.AchievementDao
import com.simplenotes.data.local.database.dao.ChallengeDao
import com.simplenotes.data.local.database.dao.EconomyDao
import com.simplenotes.data.local.database.dao.GameDao
import com.simplenotes.data.local.database.dao.ProfileDao
import com.simplenotes.data.local.database.dao.NoteDao
import com.simplenotes.data.local.database.dao.TodoDao
import com.simplenotes.data.local.database.dao.StatsDao
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
    fun provideDatabase(@ApplicationContext context: Context): SimpleNotesDatabase =
        Room.databaseBuilder(context, SimpleNotesDatabase::class.java, "simplenotes.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideGameDao(db: SimpleNotesDatabase): GameDao = db.gameDao()
    @Provides fun provideStatsDao(db: SimpleNotesDatabase): StatsDao = db.statsDao()
    @Provides fun provideAchievementDao(db: SimpleNotesDatabase): AchievementDao = db.achievementDao()
    @Provides fun provideChallengeDao(db: SimpleNotesDatabase): ChallengeDao = db.challengeDao()
    @Provides fun provideEconomyDao(db: SimpleNotesDatabase): EconomyDao = db.economyDao()
    @Provides fun provideProfileDao(db: SimpleNotesDatabase): ProfileDao = db.profileDao()
    @Provides fun provideNoteDao(db: SimpleNotesDatabase): NoteDao = db.noteDao()
    @Provides fun provideTodoDao(db: SimpleNotesDatabase): TodoDao = db.todoDao()
}
