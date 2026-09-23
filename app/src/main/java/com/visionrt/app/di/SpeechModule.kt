package com.visionrt.app.di

import android.content.Context
import com.visionrt.core.speech.Speaker
import com.visionrt.core.speech.SystemSpeaker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpeechModule {

    @Provides
    @Singleton
    fun provideSpeaker(
        @ApplicationContext context: Context,
    ): Speaker = SystemSpeaker(context)
}
