package com.visionrt.inference.postprocess

import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.Proximity
import com.visionrt.inference.manifest.ModelManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionPostProcessorTest {

    private val manifest = ModelManifest(
        modelId = "yolov8n-accessibility",
        version = "1.0.0",
        runtime = "litert",
        quantization = "int8",
        inputWidth = 320,
        inputHeight = 320,
        classes = listOf("person", "chair"),
        confidenceThreshold = 0.60f,
        nmsIouThreshold = 0.45f,
        maxDetections = 10,
        checksum = "",
        modelAsset = "model.tflite",
    )

    private class Box(
        val cx: Float = 160f,
        val cy: Float = 160f,
        val w: Float = 100f,
        val h: Float = 100f,
        val person: Float = 0.9f,
        val chair: Float = 0.1f,
    )

    private fun emptyOutput(anchors: Int) = FloatArray((4 + 2) * anchors)

    private fun put(output: FloatArray, anchors: Int, anchor: Int, box: Box) {
        output[0 * anchors + anchor] = box.cx
        output[1 * anchors + anchor] = box.cy
        output[2 * anchors + anchor] = box.w
        output[3 * anchors + anchor] = box.h
        output[4 * anchors + anchor] = box.person
        output[5 * anchors + anchor] = box.chair
    }

    @Test
    fun decodesHighConfidenceBox() {
        val anchors = 4
        val out = emptyOutput(anchors)
        put(out, anchors, 0, Box(w = 200f, h = 200f))
        val detections = DetectionPostProcessor.process(out, 6, anchors, manifest, timestampMs = 42)
        assertEquals(1, detections.size)
        val d = detections[0]
        assertEquals("person", d.label)
        assertEquals(0.9f, d.confidence, 1e-5f)
        assertEquals(HorizontalSector.CENTER, d.sector)
        assertEquals(Proximity.NEAR, d.proximity)
        assertEquals(42, d.timestampMs)
    }

    @Test
    fun filtersBelowConfidenceThreshold() {
        val anchors = 2
        val out = emptyOutput(anchors)
        put(out, anchors, 0, Box(person = 0.3f, chair = 0.2f))
        put(out, anchors, 1, Box(cx = 50f, w = 40f, h = 40f, person = 0.1f, chair = 0.05f))
        val detections = DetectionPostProcessor.process(out, 6, anchors, manifest, 1)
        assertTrue(detections.isEmpty())
    }

    @Test
    fun nmsSuppressesOverlappingDuplicates() {
        val anchors = 2
        val out = emptyOutput(anchors)
        put(out, anchors, 0, Box(w = 120f, h = 120f))
        put(out, anchors, 1, Box(cx = 162f, cy = 162f, w = 120f, h = 120f, person = 0.8f))
        val detections = DetectionPostProcessor.process(out, 6, anchors, manifest, 1)
        assertEquals(1, detections.size)
        assertEquals(0.9f, detections[0].confidence, 1e-5f)
    }

    @Test
    fun nmsKeepsNonOverlappingBoxes() {
        val anchors = 2
        val out = emptyOutput(anchors)
        put(out, anchors, 0, Box(cx = 50f, w = 80f, h = 80f))
        put(out, anchors, 1, Box(cx = 270f, w = 80f, h = 80f, person = 0.85f))
        val detections = DetectionPostProcessor.process(out, 6, anchors, manifest, 1)
        assertEquals(2, detections.size)
    }

    @Test
    fun sectorLeftForLeftSideBox() {
        val out = emptyOutput(1)
        put(out, 1, 0, Box(cx = 40f, w = 50f, h = 50f))
        val d = DetectionPostProcessor.process(out, 6, 1, manifest, 1).single()
        assertEquals(HorizontalSector.LEFT, d.sector)
    }

    @Test
    fun sectorRightForRightSideBox() {
        val out = emptyOutput(1)
        put(out, 1, 0, Box(cx = 280f, w = 50f, h = 50f))
        val d = DetectionPostProcessor.process(out, 6, 1, manifest, 1).single()
        assertEquals(HorizontalSector.RIGHT, d.sector)
    }

    @Test
    fun smallBoxIsFarProximity() {
        val out = emptyOutput(1)
        put(out, 1, 0, Box(w = 40f, h = 40f))
        val d = DetectionPostProcessor.process(out, 6, 1, manifest, 1).single()
        assertEquals(Proximity.FAR, d.proximity)
    }

    @Test
    fun respectsMaxDetections() {
        val anchors = 5
        val out = emptyOutput(anchors)
        for (i in 0 until anchors) {
            val cx = 30f + i * 70f
            put(out, anchors, i, Box(cx = cx, w = 40f, h = 40f))
        }
        val limited = manifest.copy(maxDetections = 2)
        val detections = DetectionPostProcessor.process(out, 6, anchors, limited, 1)
        assertEquals(2, detections.size)
    }
}
