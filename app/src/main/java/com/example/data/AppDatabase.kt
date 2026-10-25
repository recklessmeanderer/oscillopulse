package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.ModulationMode
import com.example.model.WaveformType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY isFactory DESC, id ASC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getCount(): Int
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [PresetEntity::class, RecordingEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "oscillopulse_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateFactoryPresets(database.presetDao())
                    }
                }
            }
        }

        suspend fun populateFactoryPresets(dao: PresetDao) {
            val defaults = listOf(
                PresetEntity(
                    name = "40 Hz Gamma Focus",
                    description = "Cognitive clarity & gamma wave brain entrainment (432Hz carrier + 40Hz square pulse)",
                    carrierFreq = 432.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 40.0f,
                    modIntensity = 1.0f,
                    modWave = WaveformType.SQUARE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isFactory = true
                ),
                PresetEntity(
                    name = "Alpha 10 Hz Relaxation",
                    description = "Flow state & calm focus with 432Hz carrier and gentle sine AM",
                    carrierFreq = 432.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 10.0f,
                    modIntensity = 0.75f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.AMPLITUDE_MODULATION.name,
                    isFactory = true
                ),
                PresetEntity(
                    name = "Theta 6 Hz Deep State",
                    description = "Deep meditation & creative visualization (216Hz carrier + 6Hz pulse)",
                    carrierFreq = 216.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 6.0f,
                    modIntensity = 0.85f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isFactory = true
                ),
                PresetEntity(
                    name = "Delta 2.5 Hz Sleep",
                    description = "Deep restorative sleep & regeneration (108Hz carrier + 2.5Hz gentle wave)",
                    carrierFreq = 108.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 2.5f,
                    modIntensity = 0.90f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isFactory = true
                ),
                PresetEntity(
                    name = "Solfeggio 528 Hz Pure",
                    description = "Pure resonance tone (528Hz continuous carrier wave)",
                    carrierFreq = 528.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 1.0f,
                    modIntensity = 0.0f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isFactory = true
                ),
                PresetEntity(
                    name = "Sub-Bass 60 Hz Strobe",
                    description = "Powerful rhythmic low-frequency sawtooth pulse",
                    carrierFreq = 60.0f,
                    carrierWave = WaveformType.SAWTOOTH.name,
                    modFreq = 8.0f,
                    modIntensity = 1.0f,
                    modWave = WaveformType.SQUARE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isFactory = true
                ),
                PresetEntity(
                    name = "Binaural 40 Hz Gamma",
                    description = "Stereo 40Hz binaural beat (412Hz Left ear + 452Hz Right ear = 40Hz beat)",
                    carrierFreq = 432.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 40.0f,
                    modIntensity = 1.0f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.BINAURAL_BEAT.name,
                    isBinauralEnabled = true,
                    leftCarrierFreq = 412.0f,
                    rightCarrierFreq = 452.0f,
                    isFactory = true
                ),
                PresetEntity(
                    name = "Harmonic Chime 216Hz",
                    description = "Additive overtone chime (216Hz carrier layered with 2x, 3x, and 4x sine harmonics)",
                    carrierFreq = 216.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 1.0f,
                    modIntensity = 0.0f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isHarmonicsEnabled = true,
                    h1Multiplier = 2,
                    h1Volume = 0.5f,
                    h2Multiplier = 3,
                    h2Volume = 0.3f,
                    h3Multiplier = 4,
                    h3Volume = 0.15f,
                    isFactory = true
                ),
                PresetEntity(
                    name = "Delta Sleep & White Noise",
                    description = "100Hz soothing carrier + 2.5Hz Delta modulation blended with a relaxing white noise floor for acoustic masking and sleep",
                    carrierFreq = 100.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 2.5f,
                    modIntensity = 0.6f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isWhiteNoiseEnabled = true,
                    whiteNoiseLevel = 0.18f,
                    isFactory = true
                )
            )
            for (p in defaults) {
                dao.insertPreset(p)
            }
        }
    }
}
