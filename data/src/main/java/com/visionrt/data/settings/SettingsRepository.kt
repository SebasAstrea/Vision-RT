package com.visionrt.data.settings

import com.visionrt.core.domain.Verbosity
import kotlinx.coroutines.flow.Flow

/**
 * User preferences owned by the data module. Nobody reads/writes the
 * DataStore directly outside this module (ARCHITECTURE.md §7).
 */
interface SettingsRepository {
    /** FR-001: explicit disclaimer acknowledgment, never bypassable. */
    val onboardingAcknowledged: Flow<Boolean>

    suspend fun setOnboardingAcknowledged(acknowledged: Boolean)

    /** FR-002: training may be skipped; can be replayed from settings. */
    val trainingCompleted: Flow<Boolean>

    suspend fun setTrainingCompleted(completed: Boolean)

    /** FR-006.5: verbosity level (persisted). */
    val verbosity: Flow<Verbosity>

    suspend fun setVerbosity(verbosity: Verbosity)
}
