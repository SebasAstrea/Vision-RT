package com.visionrt.core.common

import com.visionrt.core.common.SafePayload.SafePayloadKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SafeLogTest {

    @Test
    fun `format prefixes unqualified tags`() {
        assertEquals("VisionRT/perception: hello", SafeLog.format("perception", "hello"))
    }

    @Test
    fun `format keeps already prefixed tags`() {
        assertEquals("VisionRT/perception: hello", SafeLog.format("VisionRT/perception", "hello"))
    }

    @Test
    fun `metric renders numeric only`() {
        assertEquals("VisionRT/fps: fps=15.0fps", SafeLog.metric("fps", "fps", 15.0, "fps"))
    }

    @Test
    fun `safe renders wrapped payload`() {
        val payload = SafePayload(
            kind = SafePayloadKind.NUMERIC,
            numeric = mapOf("latencyMs" to 42)
        )
        assertEquals(
            "VisionRT/bench: run complete | latencyMs=42",
            SafeLog.safe("bench", "run complete", payload)
        )
    }

    @Test
    fun `safe rejects empty payload by design`() {
        assertThrows(IllegalArgumentException::class.java) {
            SafePayload(kind = SafePayloadKind.STATUS)
        }
    }

    @Test
    fun `rejectUnsafe fails loudly`() {
        assertThrows(UnsafePayloadException::class.java) { SafeLog.rejectUnsafe() }
    }
}
