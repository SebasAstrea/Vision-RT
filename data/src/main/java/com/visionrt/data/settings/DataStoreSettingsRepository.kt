package com.visionrt.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.visionrt.core.domain.HapticIntensity
import com.visionrt.core.domain.Verbosity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [SettingsRepository]. One instance per process (provided by
 * Hilt); the DataStore file lives under the application files dir.
 */
class DataStoreSettingsRepository(
    private val dataStore: androidx.datastore.core.DataStore<Preferences>,
) : SettingsRepository {

    override val onboardingAcknowledged: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_ACKNOWLEDGED] ?: false }

    override suspend fun setOnboardingAcknowledged(acknowledged: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_ACKNOWLEDGED] = acknowledged }
    }

    override val trainingCompleted: Flow<Boolean> =
        dataStore.data.map { it[Keys.TRAINING_COMPLETED] ?: false }

    override suspend fun setTrainingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.TRAINING_COMPLETED] = completed }
    }

    override val verbosity: Flow<Verbosity> =
        dataStore.data.map { it[Keys.VERBOSITY]?.let(Verbosity::fromKey) ?: Verbosity.NORMAL }

    override suspend fun setVerbosity(verbosity: Verbosity) {
        dataStore.edit { it[Keys.VERBOSITY] = verbosity.key }
    }

    override val speechMuted: Flow<Boolean> =
        dataStore.data.map { it[Keys.SPEECH_MUTED] ?: false }

    override suspend fun setSpeechMuted(muted: Boolean) {
        dataStore.edit { it[Keys.SPEECH_MUTED] = muted }
    }

    override val speechRate: Flow<Float> =
        dataStore.data.map { it[Keys.SPEECH_RATE] ?: DEFAULT_SPEECH_RATE }

    override suspend fun setSpeechRate(rate: Float) {
        dataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE) }
    }

    override val hapticsEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.HAPTICS_ENABLED] ?: true }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    override val hapticIntensity: Flow<HapticIntensity> =
        dataStore.data.map {
            it[Keys.HAPTIC_INTENSITY]?.let(HapticIntensity::valueOf)
                ?: HapticIntensity.MEDIUM
        }

    override suspend fun setHapticIntensity(intensity: HapticIntensity) {
        dataStore.edit { it[Keys.HAPTIC_INTENSITY] = intensity.name }
    }

    override val earconsEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.EARCONS_ENABLED] ?: true }

    override suspend fun setEarconsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.EARCONS_ENABLED] = enabled }
    }

    private object Keys {
        val ONBOARDING_ACKNOWLEDGED = booleanPreferencesKey("onboarding_acknowledged")
        val TRAINING_COMPLETED = booleanPreferencesKey("training_completed")
        val VERBOSITY = stringPreferencesKey("verbosity")
        val SPEECH_MUTED = booleanPreferencesKey("speech_muted")
        val SPEECH_RATE = floatPreferencesKey(FeedbackPreferenceKeys.SPEECH_RATE)
        val HAPTICS_ENABLED = booleanPreferencesKey(FeedbackPreferenceKeys.HAPTICS_ENABLED)
        val HAPTIC_INTENSITY = stringPreferencesKey(FeedbackPreferenceKeys.HAPTIC_INTENSITY)
        val EARCONS_ENABLED = booleanPreferencesKey(FeedbackPreferenceKeys.EARCONS_ENABLED)
    }

    private companion object {
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f
    }
}
