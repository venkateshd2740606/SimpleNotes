package com.simplenotes.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.simplenotes.data.local.database.dao.AchievementDao
import com.simplenotes.data.local.database.dao.ChallengeDao
import com.simplenotes.data.local.database.dao.EconomyDao
import com.simplenotes.data.local.database.dao.GameDao
import com.simplenotes.data.local.database.dao.ProfileDao
import com.simplenotes.data.local.database.dao.StatsDao
import com.simplenotes.data.local.database.dao.NoteDao
import com.simplenotes.data.local.database.dao.TodoDao
import com.simplenotes.data.local.database.entity.NoteEntity
import com.simplenotes.data.local.database.entity.ProfileEntity
import com.simplenotes.data.local.database.entity.TodoEntity
import com.simplenotes.data.local.database.entity.AchievementEntity
import com.simplenotes.data.local.database.entity.ChallengeEntity
import com.simplenotes.data.local.database.entity.EconomyEntity
import com.simplenotes.data.local.database.entity.GameEntity
import com.simplenotes.data.local.database.entity.StatsEntity

@Database(
    entities = [
        GameEntity::class,
        StatsEntity::class,
        AchievementEntity::class,
        ChallengeEntity::class,
        EconomyEntity::class,
        ProfileEntity::class,
        NoteEntity::class,
        TodoEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class SimpleNotesDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun statsDao(): StatsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun economyDao(): EconomyDao
    abstract fun profileDao(): ProfileDao
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao
}
