package com.visionrt.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.visionrt.core.domain.Verbosity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private object Keys {
        val ONBOARDING_ACKNOWLEDGED = booleanPreferencesKey("onboarding_acknowledged")
        val TRAINING_COMPLETED = booleanPreferencesKey("training_completed")
        val VERBOSITY = stringPreferencesKey("verbosity")
    }
}
