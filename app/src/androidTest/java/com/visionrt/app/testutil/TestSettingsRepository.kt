package com.visionrt.app.testutil

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

    object STATE {
        var onboardingAcknowledged = false
        var trainingCompleted = false
        var verbosity = Verbosity.NORMAL

        fun reset() {
            onboardingAcknowledged = false
            trainingCompleted = false
            verbosity = Verbosity.NORMAL
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
