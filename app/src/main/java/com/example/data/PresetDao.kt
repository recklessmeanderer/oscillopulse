package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room presets database.
 * Provides reactive Kotlin Flows for UI observation and suspend methods for CRUD operations.
 */
@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY isFactory DESC, id ASC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isFactory = 0 ORDER BY createdAt DESC, id DESC")
    fun getCustomPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isFactory = 1 ORDER BY id ASC")
    fun getFactoryPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    fun getPresetById(id: Long): Flow<PresetEntity?>

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun findPresetById(id: Long): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>): List<Long>

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)

    @Query("DELETE FROM presets WHERE isFactory = 0")
    suspend fun deleteAllCustomPresets()

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getCount(): Int

    @Query("DELETE FROM presets WHERE name = :name AND id != :keepId")
    suspend fun deleteDuplicatesByName(name: String, keepId: Long)

    @Query("DELETE FROM presets WHERE isFactory = 1")
    suspend fun deleteAllFactoryPresets()
}
