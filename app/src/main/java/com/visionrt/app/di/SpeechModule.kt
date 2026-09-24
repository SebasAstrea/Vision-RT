package com.visionrt.app.di

import android.content.Context
import com.visionrt.app.feedback.FeedbackSettingsBinder
import com.visionrt.core.speech.Speaker
import com.visionrt.core.speech.SystemSpeaker
import com.visionrt.data.settings.SettingsRepository
import com.visionrt.feedback.AndroidHapticPlayer
import com.visionrt.feedback.EarconPlayer
import com.visionrt.feedback.FeedbackSettings
import com.visionrt.feedback.HapticPlayer
import com.visionrt.feedback.PriorityFeedbackDispatcher
import com.visionrt.feedback.SoundPoolEarconPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object SpeechModule {

    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Provides
    @Singleton
    fun provideSpeaker(
        @ApplicationContext context: Context,
    ): Speaker = SystemSpeaker(context)

    @Provides
    @Singleton
    fun provideHapticPlayer(
        @ApplicationContext context: Context,
    ): HapticPlayer = AndroidHapticPlayer(context)

    @Provides
    @Singleton
    fun provideEarconPlayer(
        @ApplicationContext context: Context,
    ): EarconPlayer = SoundPoolEarconPlayer(context)

    /**
     * M4: priority-queue dispatcher with haptics + earcons (ARCHITECTURE §12.3).
     * Live preference updates flow through [FeedbackSettingsBinder].
     */
    @Provides
    @Singleton
    fun provideFeedbackDispatcher(
        speaker: Speaker,
        haptics: HapticPlayer,
        earcons: EarconPlayer,
        appScope: CoroutineScope,
        settings: SettingsRepository,
    ): PriorityFeedbackDispatcher {
        val initial = runBlocking {
            FeedbackSettings(
                speechMuted = settings.speechMuted.first(),
                speechRate = settings.speechRate.first(),
                hapticsEnabled = settings.hapticsEnabled.first(),
                hapticIntensity = settings.hapticIntensity.first(),
                earconsEnabled = settings.earconsEnabled.first(),
            )
        }
        val dispatcher = PriorityFeedbackDispatcher(
            speaker = speaker,
            haptics = haptics,
            earcons = earcons,
            scope = appScope,
            initialSettings = initial,
        )
        FeedbackSettingsBinder(settings, dispatcher).bind(appScope)
        return dispatcher
    }
}
