package com.naampath.colorpath3d.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "level_progress", primaryKeys = ["levelId", "mode"])
data class LevelProgressEntity(
    val levelId: Int,
    val mode: String,
    val stars: Int,
    val bestScore: Int,
    val bestTimeMs: Long,
    val bestMoves: Int,
    val completed: Boolean,
    val signature: String
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long
)

@Entity(tableName = "daily_results")
data class DailyResultEntity(
    @PrimaryKey val epochDay: Long,
    val stars: Int,
    val score: Int,
    val completed: Boolean
)

@Entity(tableName = "local_scores")
data class LocalScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val board: String,
    val score: Int,
    val timeMs: Long,
    val epochDay: Long
)

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress")
    suspend fun all(): List<LevelProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LevelProgressEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    suspend fun all(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementEntity)
}

@Dao
interface DailyDao {
    @Query("SELECT * FROM daily_results")
    suspend fun all(): List<DailyResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyResultEntity)
}

@Dao
interface ScoreDao {
    @Query("SELECT * FROM local_scores WHERE board = :board ORDER BY score DESC LIMIT 20")
    suspend fun top(board: String): List<LocalScoreEntity>

    @Insert
    suspend fun insert(entity: LocalScoreEntity)
}

@Database(
    entities = [LevelProgressEntity::class, AchievementEntity::class, DailyResultEntity::class, LocalScoreEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levels(): LevelProgressDao
    abstract fun achievements(): AchievementDao
    abstract fun daily(): DailyDao
    abstract fun scores(): ScoreDao
}
