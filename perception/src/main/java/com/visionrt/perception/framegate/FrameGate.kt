package com.visionrt.perception.framegate

import com.visionrt.perception.PerceptionFrame

/**
 * Latest-frame-only gate (ARCHITECTURE §7). Holds at most one frame; stale
 * frames are dropped by design so the pipeline always consumes the newest
 * camera frame and never queues up under a slow inference.
 */
interface FrameGate {
    fun submit(frame: PerceptionFrame)
    fun latest(): PerceptionFrame?
    fun clear()

    /** Atomically takes the pending frame, leaving the gate empty. */
    fun consume(): PerceptionFrame?
}
