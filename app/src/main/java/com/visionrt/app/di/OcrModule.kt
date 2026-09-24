package com.visionrt.app.di

import com.visionrt.app.ocr.AssistanceModesCoordinator
import com.visionrt.app.ocr.OcrLifecycleManager
import com.visionrt.core.ocr.OcrLifecycle
import com.visionrt.core.ocr.RecognizerOcrSource
import com.visionrt.core.ocr.TextReadingService
import com.visionrt.core.ocr.TextRecognizer
import com.visionrt.core.summary.ObjectSummaryService
import com.visionrt.inference.ocr.MlKitTextRecognizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideTextRecognizer(impl: MlKitTextRecognizer): TextRecognizer = impl

    @Provides
    @Singleton
    fun provideOcrLifecycle(impl: OcrLifecycleManager): OcrLifecycle = impl

    @Provides
    @Singleton
    fun provideRecognizerOcrSource(
        recognizer: TextRecognizer,
        lifecycle: OcrLifecycle,
    ): RecognizerOcrSource = RecognizerOcrSource(recognizer, lifecycle) { null }

    @Provides
    @Singleton
    fun provideObjectSummaryService(
        coordinator: AssistanceModesCoordinator,
    ): ObjectSummaryService = coordinator

    @Provides
    @Singleton
    fun provideTextReadingService(
        coordinator: AssistanceModesCoordinator,
    ): TextReadingService = coordinator
}
