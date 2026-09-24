package com.visionrt.app.di

import com.visionrt.app.diagnostics.AndroidDeviceProfileProvider
import com.visionrt.app.diagnostics.DetectorBenchmarkRunner
import com.visionrt.app.memory.AndroidMemoryMonitor
import com.visionrt.core.diagnostics.DiagnosticsPort
import com.visionrt.core.domain.DeviceProfileProvider
import com.visionrt.core.memory.MemoryPressureBus
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsModule {

    @Binds
    @Singleton
    abstract fun bindDeviceProfileProvider(
        impl: AndroidDeviceProfileProvider,
    ): DeviceProfileProvider

    @Binds
    @Singleton
    abstract fun bindDiagnosticsPort(
        impl: DetectorBenchmarkRunner,
    ): DiagnosticsPort

    companion object {
        @Provides
        @Singleton
        fun provideMemoryPressureBus(): MemoryPressureBus = MemoryPressureBus()

        @Provides
        @Singleton
        fun provideMemoryMonitor(bus: MemoryPressureBus): AndroidMemoryMonitor =
            AndroidMemoryMonitor(bus)
    }
}
