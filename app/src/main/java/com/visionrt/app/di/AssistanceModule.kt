package com.visionrt.app.di

import android.content.Context
import com.visionrt.app.assistance.CameraDetectionSource
import com.visionrt.app.assistance.ObstacleAssistanceCoordinator
import com.visionrt.core.assistance.AssistanceController
import com.visionrt.core.orchestration.AlertPipeline
import com.visionrt.core.orchestration.AlertPolicy
import com.visionrt.core.orchestration.DetectionSource
import com.visionrt.core.orchestration.FeedbackPort
import com.visionrt.core.orchestration.ModeController
import com.visionrt.feedback.PriorityFeedbackDispatcher
import com.visionrt.inference.litert.LiteRtObjectDetector
import com.visionrt.inference.runtimeapi.ObjectDetector
import com.visionrt.perception.camera.CameraFrameSource
import com.visionrt.perception.framegate.LatestOnlyFrameGate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AssistanceModule {

    @Provides
    @Singleton
    fun provideFrameGate(): LatestOnlyFrameGate = LatestOnlyFrameGate()

    @Provides
    @Singleton
    fun provideObjectDetector(
        @ApplicationContext context: Context,
    ): ObjectDetector = LiteRtObjectDetector(context.assets)

    @Provides
    @Singleton
    fun provideCameraFrameSource(
        @ApplicationContext context: Context,
        frameGate: LatestOnlyFrameGate,
    ): CameraFrameSource = CameraFrameSource(context, frameGate)

    @Provides
    @Singleton
    fun provideFeedbackPort(dispatcher: PriorityFeedbackDispatcher): FeedbackPort = dispatcher

    @Provides
    @Singleton
    fun provideModeController(feedback: FeedbackPort): ModeController =
        ModeController(feedback)

    @Provides
    @Singleton
    fun provideAlertPolicy(): AlertPolicy = AlertPolicy()

    @Provides
    @Singleton
    fun provideAlertPipeline(
        policy: AlertPolicy,
        feedback: FeedbackPort,
    ): AlertPipeline = AlertPipeline(policy, feedback)

    @Provides
    @Singleton
    fun provideDetectionSource(source: CameraDetectionSource): DetectionSource = source

    @Provides
    @Singleton
    fun provideAssistanceController(controller: ObstacleAssistanceCoordinator): AssistanceController =
        controller
}
