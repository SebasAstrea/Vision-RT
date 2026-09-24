package com.visionrt.app

import android.app.Application
import com.visionrt.app.memory.AndroidMemoryMonitor
import com.visionrt.app.memory.bindMemoryMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VisionRTApplication : Application() {

    @Inject
    lateinit var memoryMonitor: AndroidMemoryMonitor

    override fun onCreate() {
        super.onCreate()
        bindMemoryMonitor(memoryMonitor)
    }
}
