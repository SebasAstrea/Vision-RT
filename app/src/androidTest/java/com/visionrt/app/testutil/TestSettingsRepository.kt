package com.visionrt.app.testutil

import com.visionrt.core.domain.HapticIntensity
import com.visionrt.core.domain.Verbosity
import com.visionrt.data.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory SettingsRepository for instrumented tests. State is held in the
 * companion so @Before can reset it deterministically between tests without
 * killing the instrumentation process (an in-disk store would need pm clear,
 * which also kills the test process).
 */
class TestSettingsRepository : SettingsRepository {

    private val ackFlow = MutableStateFlow(STATE.onboardingAcknowledged)
    private val trainingFlow = MutableStateFlow(STATE.trainingCompleted)
    private val verbosityFlow = MutableStateFlow(STATE.verbosity)
    private val speechMutedFlow = MutableStateFlow(STATE.speechMuted)
    private val speechRateFlow = MutableStateFlow(STATE.speechRate)
    private val hapticsEnabledFlow = MutableStateFlow(STATE.hapticsEnabled)
    private val hapticIntensityFlow = MutableStateFlow(STATE.hapticIntensity)
    private val earconsEnabledFlow = MutableStateFlow(STATE.earconsEnabled)

    override val onboardingAcknowledged: StateFlow<Boolean> = ackFlow.asStateFlow()

    override suspend fun setOnboardingAcknowledged(acknowledged: Boolean) {
        STATE.onboardingAcknowledged = acknowledged
        ackFlow.value = acknowledged
    }

    override val trainingCompleted: StateFlow<Boolean> = trainingFlow.asStateFlow()

    override suspend fun setTrainingCompleted(completed: Boolean) {
        STATE.trainingCompleted = completed
        trainingFlow.value = completed
    }

    override val verbosity: StateFlow<Verbosity> = verbosityFlow.asStateFlow()

    override suspend fun setVerbosity(verbosity: Verbosity) {
        STATE.verbosity = verbosity
        verbosityFlow.value = verbosity
    }

    override val speechMuted: StateFlow<Boolean> = speechMutedFlow.asStateFlow()

    override suspend fun setSpeechMuted(muted: Boolean) {
        STATE.speechMuted = muted
        speechMutedFlow.value = muted
    }

    override val speechRate: StateFlow<Float> = speechRateFlow.asStateFlow()

    override suspend fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        STATE.speechRate = clamped
        speechRateFlow.value = clamped
    }

    override val hapticsEnabled: StateFlow<Boolean> = hapticsEnabledFlow.asStateFlow()

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        STATE.hapticsEnabled = enabled
        hapticsEnabledFlow.value = enabled
    }

    override val hapticIntensity: StateFlow<HapticIntensity> = hapticIntensityFlow.asStateFlow()

    override suspend fun setHapticIntensity(intensity: HapticIntensity) {
        STATE.hapticIntensity = intensity
        hapticIntensityFlow.value = intensity
    }

    override val earconsEnabled: StateFlow<Boolean> = earconsEnabledFlow.asStateFlow()

    override suspend fun setEarconsEnabled(enabled: Boolean) {
        STATE.earconsEnabled = enabled
        earconsEnabledFlow.value = enabled
    }

    object STATE {
        var onboardingAcknowledged = false
        var trainingCompleted = false
        var verbosity = Verbosity.NORMAL
        var speechMuted = false
        var speechRate = 1.0f
        var hapticsEnabled = true
        var hapticIntensity = HapticIntensity.MEDIUM
        var earconsEnabled = true

        fun reset() {
            onboardingAcknowledged = false
            trainingCompleted = false
            verbosity = Verbosity.NORMAL
            speechMuted = false
            speechRate = 1.0f
            hapticsEnabled = true
            hapticIntensity = HapticIntensity.MEDIUM
            earconsEnabled = true
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object TestSettingsModule {

    @Provides
    @Singleton
    fun provideTestSettingsRepository(): SettingsRepository = TestSettingsRepository()
}
