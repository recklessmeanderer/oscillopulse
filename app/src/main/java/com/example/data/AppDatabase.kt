package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.ChordPreset
import com.example.model.ModulationMode
import com.example.model.NoiseColor
import com.example.model.WaveformType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

@Database(entities = [PresetEntity::class, RecordingEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var REPOSITORY_INSTANCE: PresetRepository? = null

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

        fun getPresetRepository(context: Context): PresetRepository {
            return REPOSITORY_INSTANCE ?: synchronized(this) {
                val db = getDatabase(context)
                val repo = PresetRepositoryImpl(db.presetDao())
                REPOSITORY_INSTANCE = repo
                repo
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
                    id = 1L,
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
                    id = 2L,
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
                    id = 3L,
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
                    id = 4L,
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
                    id = 5L,
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
                    id = 6L,
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
                    id = 7L,
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
                    id = 8L,
                    name = "Delta Sleep & White Noise",
                    description = "100Hz soothing carrier + 2.5Hz Delta modulation blended with a relaxing white noise floor",
                    carrierFreq = 100.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 2.5f,
                    modIntensity = 0.6f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isWhiteNoiseEnabled = true,
                    whiteNoiseLevel = 0.18f,
                    noiseColor = "WHITE",
                    isFactory = true
                ),
                PresetEntity(
                    id = 9L,
                    name = "Deep Brown Noise & Theta",
                    description = "Deep warm brown rumble resembling ocean surf blended with 108Hz carrier and 6Hz Theta entrainment",
                    carrierFreq = 108.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 6.0f,
                    modIntensity = 0.7f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.ISOCHRONIC_PULSE.name,
                    isWhiteNoiseEnabled = true,
                    whiteNoiseLevel = 0.22f,
                    noiseColor = "BROWN",
                    isFactory = true
                ),
                PresetEntity(
                    id = 10L,
                    name = "Solfeggio Harmonic Triad",
                    description = "Multi-tone chord stack: 432Hz fundamental + 528Hz + 639Hz sacred resonance triad",
                    carrierFreq = 432.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 10.0f,
                    modIntensity = 0.4f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.AMPLITUDE_MODULATION.name,
                    isLayeringEnabled = true,
                    chordPreset = "SOLFEGGIO_TRIAD",
                    layer2Freq = 528.0f,
                    layer2Gain = 0.45f,
                    layer2Wave = WaveformType.SINE.name,
                    layer3Freq = 639.0f,
                    layer3Gain = 0.40f,
                    layer3Wave = WaveformType.SINE.name,
                    isFactory = true
                ),
                PresetEntity(
                    id = 11L,
                    name = "Pink Noise Zen Rainfall",
                    description = "Gentle natural 1/f pink noise with 432Hz carrier and smooth 10Hz Alpha relaxation wave",
                    carrierFreq = 432.0f,
                    carrierWave = WaveformType.SINE.name,
                    modFreq = 10.0f,
                    modIntensity = 0.5f,
                    modWave = WaveformType.SINE.name,
                    modMode = ModulationMode.AMPLITUDE_MODULATION.name,
                    isWhiteNoiseEnabled = true,
                    whiteNoiseLevel = 0.18f,
                    noiseColor = "PINK",
                    isFactory = true
                )
            )
            for (p in defaults) {
                dao.insertPreset(p)
                dao.deleteDuplicatesByName(p.name, p.id)
            }
        }
    }
}
