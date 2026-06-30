package com.simplenotes.data.repository

import com.simplenotes.R
import com.simplenotes.data.local.database.dao.AchievementDao
import com.simplenotes.data.local.database.dao.ChallengeDao
import com.simplenotes.data.local.database.dao.EconomyDao
import com.simplenotes.data.local.database.dao.GameDao
import com.simplenotes.data.local.database.dao.StatsDao
import com.simplenotes.data.local.database.entity.AchievementEntity
import com.simplenotes.data.mapper.DataMappers
import com.simplenotes.domain.model.Achievement
import com.simplenotes.domain.model.ChallengeRecord
import com.simplenotes.domain.model.ChallengeType
import com.simplenotes.domain.model.Difficulty
import com.simplenotes.domain.model.EconomyState
import com.simplenotes.domain.model.GameStatus
import com.simplenotes.domain.model.SimpleNotesGame
import com.simplenotes.domain.model.PuzzleProfile
import com.simplenotes.domain.model.SimpleNotesLevel
import com.simplenotes.domain.model.UserStats
import com.simplenotes.domain.repository.ChallengeRepository
import com.simplenotes.domain.repository.GameRepository
import com.simplenotes.domain.repository.ProgressionRepository
import com.simplenotes.data.local.database.dao.ProfileDao
import com.simplenotes.engine.PuzzleProfileEngine
import com.simplenotes.engine.SimpleNotesEngine
import com.simplenotes.engine.SimpleNotesGenerator
import com.simplenotes.engine.TutorialLevels
import com.simplenotes.util.DateUtils
import com.simplenotes.util.ProgressionCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor(
    private val gameDao: GameDao,
    private val profileDao: ProfileDao
) : GameRepository {

    override suspend fun createNewGame(difficulty: Difficulty, levelNumber: Int): SimpleNotesGame {
        val profile = DataMappers.fromProfileEntity(profileDao.get())
        val generationProfile = PuzzleProfileEngine.adaptiveGenerationProfile(profile)
        val seed = SimpleNotesGenerator.seedFromLevelNumber(levelNumber, difficulty)
        val level = SimpleNotesGenerator.generate(seed, levelNumber, difficulty, generationProfile)
        return SimpleNotesEngine.createInitialGame(level)
    }

    override suspend fun createGameFromSeed(
        seed: Long,
        levelNumber: Int,
        difficulty: Difficulty
    ): SimpleNotesGame {
        val level = SimpleNotesGenerator.generate(seed, levelNumber, difficulty)
        return SimpleNotesEngine.createInitialGame(level)
    }

    override suspend fun createTutorialGame(tutorialIndex: Int): SimpleNotesGame? {
        val level = TutorialLevels.getTutorialLevel(tutorialIndex) ?: return null
        return SimpleNotesEngine.createInitialGame(level)
    }

    override suspend fun createEndlessGame(wave: Int): SimpleNotesGame {
        val seed = wave.toLong() * 7919L + System.currentTimeMillis() % 1000
        val level = SimpleNotesGenerator.generate(seed, wave, Difficulty.ENDLESS)
        return SimpleNotesEngine.createInitialGame(level)
    }

    override suspend fun saveGame(game: SimpleNotesGame): Long {
        val entity = DataMappers.toEntity(game)
        return if (game.id == 0L) gameDao.insert(entity) else {
            gameDao.update(entity.copy(id = game.id))
            game.id
        }
    }

    override suspend fun getGame(gameId: Long): SimpleNotesGame? =
        gameDao.getById(gameId)?.let { DataMappers.fromEntity(it) }

    override suspend fun getInProgressGame(): SimpleNotesGame? =
        gameDao.getInProgress()?.let { DataMappers.fromEntity(it) }

    override fun observeInProgressGame(): Flow<SimpleNotesGame?> =
        gameDao.observeInProgress().map { it?.let { e -> DataMappers.fromEntity(e) } }

    override suspend fun completeGame(game: SimpleNotesGame): SimpleNotesGame {
        val completed = game.copy(
            status = GameStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        saveGame(completed)
        return completed
    }

    override suspend fun abandonGame(gameId: Long) = gameDao.abandon(gameId)

    override suspend fun getLevel(seed: Long, levelNumber: Int, difficulty: Difficulty): SimpleNotesLevel =
        SimpleNotesGenerator.generate(seed, levelNumber, difficulty)
}

@Singleton
class ChallengeRepositoryImpl @Inject constructor(
    private val challengeDao: ChallengeDao,
    private val gameRepository: GameRepository
) : ChallengeRepository {

    override suspend fun getChallenge(type: ChallengeType, key: String): ChallengeRecord? =
        challengeDao.getByKey(key)?.let { DataMappers.fromChallengeEntity(it) }

    override suspend fun createChallenge(type: ChallengeType, key: String, difficulty: Difficulty): ChallengeRecord {
        val seed = key.hashCode().toLong() * 31 + type.ordinal
        val record = ChallengeRecord(
            key = key,
            type = type,
            seed = seed,
            difficulty = difficulty,
            rewardCoins = rewardCoinsFor(type),
            rewardXp = rewardXpFor(type, difficulty)
        )
        challengeDao.insert(DataMappers.toChallengeEntity(record))
        return record
    }

    override suspend fun resolveActiveChallenge(type: ChallengeType): ChallengeRecord {
        val key = challengeKeyFor(type)
        val difficulty = challengeDifficultyFor(type)
        return getChallenge(type, key) ?: createChallenge(type, key, difficulty)
    }

    override fun observeActiveChallenge(type: ChallengeType): Flow<ChallengeRecord?> {
        val key = challengeKeyFor(type)
        return challengeDao.observeByKey(key).map { entity ->
            entity?.let { DataMappers.fromChallengeEntity(it) }
        }
    }

    override suspend fun completeChallenge(
        record: ChallengeRecord,
        timeSeconds: Long,
        moves: Int
    ): ChallengeRecord {
        val completed = record.copy(
            isCompleted = true,
            completionTime = timeSeconds,
            moves = moves
        )
        challengeDao.update(DataMappers.toChallengeEntity(completed))
        return completed
    }

    override fun observeChallengeHistory(type: ChallengeType): Flow<List<ChallengeRecord>> =
        challengeDao.observeByType(type.name).map { list ->
            list.map { DataMappers.fromChallengeEntity(it) }
        }

    override suspend fun getCurrentStreak(type: ChallengeType): Int {
        val history = challengeDao.getCompletedHistory(type.name)
        return DateUtils.calculateStreak(history.map { it.key })
    }

    override suspend fun getChallengeGame(record: ChallengeRecord): SimpleNotesGame {
        val levelNumber = record.key.hashCode().and(0x7FFFFFFF) % 5000 + 1
        val level = SimpleNotesGenerator.generateForChallenge(
            record.seed, levelNumber, record.difficulty
        ).copy(challengeType = record.type, challengeKey = record.key)
        return SimpleNotesEngine.createInitialGame(level)
    }

    private fun rewardCoinsFor(type: ChallengeType) = when (type) {
        ChallengeType.DAILY -> 25
        ChallengeType.WEEKLY -> 100
        ChallengeType.MONTHLY -> 500
    }

    private fun rewardXpFor(type: ChallengeType, difficulty: Difficulty): Int =
        (rewardCoinsFor(type) * difficulty.xpMultiplier).toInt()

    private fun challengeKeyFor(type: ChallengeType): String = when (type) {
        ChallengeType.DAILY -> DateUtils.todayKey()
        ChallengeType.WEEKLY -> DateUtils.weekKey()
        ChallengeType.MONTHLY -> DateUtils.monthKey()
    }

    private fun challengeDifficultyFor(type: ChallengeType): Difficulty = when (type) {
        ChallengeType.DAILY -> Difficulty.MEDIUM
        ChallengeType.WEEKLY -> Difficulty.HARD
        ChallengeType.MONTHLY -> Difficulty.EXPERT
    }
}

@Singleton
class ProgressionRepositoryImpl @Inject constructor(
    private val statsDao: StatsDao,
    private val achievementDao: AchievementDao,
    private val economyDao: EconomyDao,
    private val profileDao: ProfileDao
) : ProgressionRepository {

    private val achievementDefs = listOf(
        Achievement("first_win", R.string.achievement_first_win, R.string.achievement_first_win_desc, "star", 50, 10),
        Achievement("ten_wins", R.string.achievement_ten_wins, R.string.achievement_ten_wins_desc, "emoji_events", 100, 25),
        Achievement("perfect_game", R.string.achievement_perfect, R.string.achievement_perfect_desc, "verified", 150, 50),
        Achievement("streak_7", R.string.achievement_streak_7, R.string.achievement_streak_7_desc, "local_fire_department", 200, 75),
        Achievement("master_level", R.string.achievement_master, R.string.achievement_master_desc, "military_tech", 500, 200),
        Achievement("endless_10", R.string.achievement_endless, R.string.achievement_endless_desc, "all_inclusive", 300, 100),
        Achievement("multiplayer_win", R.string.achievement_multiplayer, R.string.achievement_multiplayer_desc, "groups", 100, 30),
        Achievement("tutorial_complete", R.string.achievement_tutorial, R.string.achievement_tutorial_desc, "school", 75, 20)
    )

    init {
        // Achievements seeded on first access
    }

    override fun observeStats(): Flow<UserStats> =
        statsDao.observe().map { DataMappers.fromStatsEntity(it) }

    override suspend fun getStats(): UserStats =
        DataMappers.fromStatsEntity(statsDao.get())

    override fun observePuzzleProfile(): Flow<PuzzleProfile> =
        profileDao.observe().map { DataMappers.fromProfileEntity(it) }

    override suspend fun getPuzzleProfile(): PuzzleProfile =
        DataMappers.fromProfileEntity(profileDao.get())

    override suspend fun grantChallengeRewards(rewardCoins: Int, rewardXp: Int) {
        if (rewardCoins > 0) earnCoins(rewardCoins)
        if (rewardXp <= 0) return
        val stats = getStats()
        val newXp = stats.xpPoints + rewardXp
        statsDao.insert(
            DataMappers.toStatsEntity(
                stats.copy(
                    xpPoints = newXp,
                    level = ProgressionCalculator.levelFromXp(newXp)
                )
            )
        )
    }

    override suspend fun updateStatsAfterGame(game: SimpleNotesGame) {
        val current = getStats()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val won = game.status == GameStatus.COMPLETED
        val newStreak = if (won && (current.lastPlayedDate == today || DateUtils.isConsecutiveDay(current.lastPlayedDate, today))) {
            if (current.lastPlayedDate == today) current.currentStreak else current.currentStreak + 1
        } else if (won) 1 else 0

        val xpGain = if (won) ProgressionCalculator.xpForGame(game) else 0
        val newXp = current.xpPoints + xpGain
        val newLevel = ProgressionCalculator.levelFromXp(newXp)

        val updated = current.copy(
            gamesPlayed = current.gamesPlayed + 1,
            gamesWon = current.gamesWon + if (won) 1 else 0,
            gamesAbandoned = current.gamesAbandoned + if (game.status == GameStatus.FAILED) 1 else 0,
            totalPlayTimeSeconds = current.totalPlayTimeSeconds + game.elapsedSeconds,
            fastestTimeBeginner = updateFastest(current.fastestTimeBeginner, game, Difficulty.BEGINNER),
            fastestTimeEasy = updateFastest(current.fastestTimeEasy, game, Difficulty.EASY),
            fastestTimeMedium = updateFastest(current.fastestTimeMedium, game, Difficulty.MEDIUM),
            fastestTimeHard = updateFastest(current.fastestTimeHard, game, Difficulty.HARD),
            fastestTimeExpert = updateFastest(current.fastestTimeExpert, game, Difficulty.EXPERT),
            fastestTimeMaster = updateFastest(current.fastestTimeMaster, game, Difficulty.MASTER),
            currentStreak = newStreak,
            longestStreak = maxOf(current.longestStreak, newStreak),
            lastPlayedDate = if (won) today else current.lastPlayedDate,
            xpPoints = newXp,
            level = newLevel,
            hintsUsedTotal = current.hintsUsedTotal + game.hintsUsed,
            perfectGames = current.perfectGames + if (won && game.hintsUsed == 0) 1 else 0,
            poursTotal = current.poursTotal + game.moves,
            endlessHighScore = if (game.level.isEndless && won) {
                maxOf(current.endlessHighScore, game.level.levelNumber)
            } else current.endlessHighScore
        )
        statsDao.insert(DataMappers.toStatsEntity(updated))

        if (won) {
            earnCoins(ProgressionCalculator.coinsForGame(game))
            updatePuzzleProfile(game)
        }
    }

    private suspend fun updatePuzzleProfile(game: SimpleNotesGame) {
        val current = getPuzzleProfile()
        val updatedMetrics = PuzzleProfileEngine.updateMetrics(current.metrics, game)
        val updatedProfile = PuzzleProfileEngine.buildProfile(updatedMetrics)
        profileDao.insert(DataMappers.toProfileEntity(updatedProfile))
    }

    override fun observeAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAll().map { entities ->
            val map = entities.associateBy { it.id }
            achievementDefs.map { def -> DataMappers.mergeAchievement(def, map[def.id]) }
        }

    override suspend fun checkAndUnlockAchievements(
        game: SimpleNotesGame,
        sameDevicePlayed: Boolean
    ): List<Achievement> {
        ensureAchievementsSeeded()
        val stats = getStats()
        val existing = achievementDao.getAll()
        val unlocked = mutableListOf<Achievement>()

        suspend fun tryUnlock(id: String, condition: Boolean, progress: Int = 1) {
            val def = achievementDefs.find { it.id == id } ?: return
            val entity = existing.find { it.id == id }
            if (entity?.isUnlocked == true) return
            val newEntity = AchievementEntity(
                id = id,
                isUnlocked = condition,
                unlockedAt = if (condition) System.currentTimeMillis() else null,
                progress = progress
            )
            achievementDao.update(newEntity)
            if (condition) unlocked.add(def.copy(isUnlocked = true, unlockedAt = newEntity.unlockedAt))
        }

        tryUnlock("first_win", stats.gamesWon >= 1)
        tryUnlock("ten_wins", stats.gamesWon >= 10, stats.gamesWon)
        tryUnlock("perfect_game", game.hintsUsed == 0 && game.status == GameStatus.COMPLETED)
        tryUnlock("streak_7", stats.currentStreak >= 7, stats.currentStreak)
        tryUnlock("master_level", stats.level >= 50, stats.level)
        tryUnlock("endless_10", stats.endlessHighScore >= 10, stats.endlessHighScore)
        tryUnlock("tutorial_complete", game.level.isTutorial && game.status == GameStatus.COMPLETED)
        tryUnlock("multiplayer_win", sameDevicePlayed)

        unlocked.forEach { achievement ->
            earnCoins(achievement.coinReward)
        }
        return unlocked
    }

    override fun observeEconomy(): Flow<EconomyState> =
        economyDao.observe().map { DataMappers.fromEconomyEntity(it) }

    override suspend fun getEconomy(): EconomyState =
        DataMappers.fromEconomyEntity(economyDao.get())

    override suspend fun spendCoins(amount: Int): Boolean {
        val current = getEconomy()
        if (current.coins < amount) return false
        val updated = current.copy(
            coins = current.coins - amount,
            totalCoinsSpent = current.totalCoinsSpent + amount
        )
        economyDao.insert(DataMappers.toEconomyEntity(updated))
        return true
    }

    override suspend fun earnCoins(amount: Int) {
        val current = getEconomy()
        val updated = current.copy(
            coins = current.coins + amount,
            totalCoinsEarned = current.totalCoinsEarned + amount
        )
        economyDao.insert(DataMappers.toEconomyEntity(updated))
    }

    override suspend fun unlockTheme(themeId: String): Boolean {
        val current = getEconomy()
        if (themeId in current.unlockedThemeIds) return true
        val cost = 150
        if (!spendCoins(cost)) return false
        val afterSpend = getEconomy()
        val updated = afterSpend.copy(unlockedThemeIds = afterSpend.unlockedThemeIds + themeId)
        economyDao.insert(DataMappers.toEconomyEntity(updated))
        return true
    }

    private suspend fun ensureAchievementsSeeded() {
        val existing = achievementDao.getAll()
        if (existing.isEmpty()) {
            achievementDao.insertAll(
                achievementDefs.map {
                    AchievementEntity(id = it.id, isUnlocked = false, unlockedAt = null, progress = 0)
                }
            )
        }
    }

    private fun updateFastest(current: Long, game: SimpleNotesGame, difficulty: Difficulty): Long {
        if (game.status != GameStatus.COMPLETED || game.level.difficulty != difficulty) return current
        return minOf(current, game.elapsedSeconds)
    }
}

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: com.simplenotes.data.local.PreferencesDataStore
) : com.simplenotes.domain.repository.PreferencesRepository {

    override fun getUserPreferences() = dataStore.preferencesFlow

    override suspend fun updatePreferences(
        transform: (com.simplenotes.domain.model.UserPreferences) -> com.simplenotes.domain.model.UserPreferences
    ) = dataStore.update(transform)

    override suspend fun getCampaignLevel(difficulty: Difficulty): Int =
        dataStore.getCampaignLevel(difficulty)

    override suspend fun advanceCampaignLevel(difficulty: Difficulty): Int =
        dataStore.advanceCampaignLevel(difficulty)
}
