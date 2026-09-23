package com.visionrt.perception.framegate

import com.visionrt.perception.PerceptionFrame

/**
 * Latest-frame-only [FrameGate] (ARCHITECTURE §7, §10.2). Thread-safe:
 * camera analyzers [submit] from a background executor while the inference
 * loop [latest]-consumes; a new submit while unread replaces the previous
 * frame so a slow detector never queues backlog.
 */
class LatestOnlyFrameGate : FrameGate {

    private val lock = Any()
    private var frame: PerceptionFrame? = null

    override fun submit(frame: PerceptionFrame) {
        synchronized(lock) {
            this.frame = frame
        }
    }

    override fun latest(): PerceptionFrame? = synchronized(lock) { frame }

    override fun clear() {
        synchronized(lock) {
            frame = null
        }
    }

    /** Atomically takes the pending frame, leaving the gate empty. */
    override fun consume(): PerceptionFrame? = synchronized(lock) {
        val result = frame
        frame = null
        result
    }

    /** Alias used by orchestration loops: take-and-clear. */
    fun consumeLatest(): PerceptionFrame? = consume()
}
