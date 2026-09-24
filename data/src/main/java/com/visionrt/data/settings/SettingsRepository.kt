package com.visionrt.data.settings

import com.visionrt.core.domain.HapticIntensity
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

    /** FR-004/FR-010: app-level speech mute (screen narration + alerts). */
    val speechMuted: Flow<Boolean>

    suspend fun setSpeechMuted(muted: Boolean)

    /** FR-010.5: TTS rate override in 0.5x–2.0x (default 1.0). */
    val speechRate: Flow<Float>

    suspend fun setSpeechRate(rate: Float)

    /** FR-011.6: haptic alerts on/off (default on). */
    val hapticsEnabled: Flow<Boolean>

    suspend fun setHapticsEnabled(enabled: Boolean)

    /** FR-011.5 / FR-012.3: haptic intensity (default MEDIUM). */
    val hapticIntensity: Flow<HapticIntensity>

    suspend fun setHapticIntensity(intensity: HapticIntensity)

    /** FR-012.4: non-speech earcons on/off (default on). */
    val earconsEnabled: Flow<Boolean>

    suspend fun setEarconsEnabled(enabled: Boolean)
}
