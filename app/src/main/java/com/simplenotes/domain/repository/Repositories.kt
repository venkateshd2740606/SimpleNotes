package com.simplenotes.domain.repository

import com.simplenotes.domain.model.Achievement
import com.simplenotes.domain.model.ChallengeRecord
import com.simplenotes.domain.model.ChallengeType
import com.simplenotes.domain.model.SimpleNotesGame
import com.simplenotes.domain.model.SimpleNotesLevel
import com.simplenotes.domain.model.Difficulty
import com.simplenotes.domain.model.EconomyState
import com.simplenotes.domain.model.PuzzleProfile
import com.simplenotes.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun createNewGame(difficulty: Difficulty, levelNumber: Int): SimpleNotesGame
    suspend fun createGameFromSeed(seed: Long, levelNumber: Int, difficulty: Difficulty): SimpleNotesGame
    suspend fun createTutorialGame(tutorialIndex: Int): SimpleNotesGame?
    suspend fun createEndlessGame(wave: Int): SimpleNotesGame
    suspend fun saveGame(game: SimpleNotesGame): Long
    suspend fun getGame(gameId: Long): SimpleNotesGame?
    suspend fun getInProgressGame(): SimpleNotesGame?
    fun observeInProgressGame(): Flow<SimpleNotesGame?>
    suspend fun completeGame(game: SimpleNotesGame): SimpleNotesGame
    suspend fun abandonGame(gameId: Long)
    suspend fun getLevel(seed: Long, levelNumber: Int, difficulty: Difficulty): SimpleNotesLevel
}

interface ChallengeRepository {
    suspend fun getChallenge(type: ChallengeType, key: String): ChallengeRecord?
    suspend fun createChallenge(type: ChallengeType, key: String, difficulty: Difficulty): ChallengeRecord
    suspend fun resolveActiveChallenge(type: ChallengeType): ChallengeRecord
    fun observeActiveChallenge(type: ChallengeType): Flow<ChallengeRecord?>
    suspend fun completeChallenge(record: ChallengeRecord, timeSeconds: Long, moves: Int): ChallengeRecord
    fun observeChallengeHistory(type: ChallengeType): Flow<List<ChallengeRecord>>
    suspend fun getCurrentStreak(type: ChallengeType): Int
    suspend fun getChallengeGame(record: ChallengeRecord): SimpleNotesGame
}

interface ProgressionRepository {
    fun observeStats(): Flow<UserStats>
    suspend fun getStats(): UserStats
    suspend fun updateStatsAfterGame(game: SimpleNotesGame)
    suspend fun grantChallengeRewards(rewardCoins: Int, rewardXp: Int)
    fun observePuzzleProfile(): Flow<PuzzleProfile>
    suspend fun getPuzzleProfile(): PuzzleProfile
    fun observeAchievements(): Flow<List<Achievement>>
    suspend fun checkAndUnlockAchievements(
        game: SimpleNotesGame,
        sameDevicePlayed: Boolean = false
    ): List<Achievement>
    fun observeEconomy(): Flow<EconomyState>
    suspend fun getEconomy(): EconomyState
    suspend fun spendCoins(amount: Int): Boolean
    suspend fun earnCoins(amount: Int)
    suspend fun unlockTheme(themeId: String): Boolean
}

interface PreferencesRepository {
    fun getUserPreferences(): Flow<com.simplenotes.domain.model.UserPreferences>
    suspend fun updatePreferences(transform: (com.simplenotes.domain.model.UserPreferences) -> com.simplenotes.domain.model.UserPreferences)
    suspend fun getCampaignLevel(difficulty: Difficulty): Int
    suspend fun advanceCampaignLevel(difficulty: Difficulty): Int
}
