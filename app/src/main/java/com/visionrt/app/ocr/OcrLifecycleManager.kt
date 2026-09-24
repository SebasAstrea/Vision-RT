package com.visionrt.app.ocr

import com.visionrt.core.common.SafeLogger
import com.visionrt.core.ocr.OcrLifecycle
import com.visionrt.core.ocr.TextRecognizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * OR-002.7: lazy-load [TextRecognizer] and unload after
 * [OcrLifecycle.IDLE_UNLOAD_MS] without use. Single mutex keeps load/unload
 * exclusive so detector + OCR never both stay resident under race (OR-002.6).
 */
@Singleton
class OcrLifecycleManager @Inject constructor(
    private val recognizer: TextRecognizer,
) : OcrLifecycle {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var idleJob: Job? = null

    @Volatile
    private var loaded: Boolean = false

    override suspend fun ensureLoaded(): Result<Unit> = mutex.withLock {
        runCatching {
            if (loaded && recognizer.isLoaded()) return@runCatching
            recognizer.load().getOrThrow()
            loaded = true
            scheduleIdleUnload()
            SafeLogger.i(TAG, "OCR loaded")
        }
    }

    override fun touch() {
        scheduleIdleUnload()
    }

    override suspend fun unload() {
        mutex.withLock {
            idleJob?.cancel()
            idleJob = null
            if (loaded || recognizer.isLoaded()) {
                recognizer.unload()
                loaded = false
                SafeLogger.i(TAG, "OCR unloaded after idle")
            }
        }
    }

    override fun isLoaded(): Boolean = loaded && recognizer.isLoaded()

    private fun scheduleIdleUnload() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(OcrLifecycle.IDLE_UNLOAD_MS)
            runCatching { unload() }.onFailure {
                SafeLogger.w(TAG, "Idle unload failed: ${it.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "OcrLifecycle"
    }
}
