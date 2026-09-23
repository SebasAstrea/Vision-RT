package com.visionrt.inference.manifest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelManifestJsonTest {

    private val validJson = """
        {
          "model_id": "yolov8n-accessibility",
          "version": "1.0.0",
          "runtime": "litert",
          "quantization": "int8",
          "input_width": 320,
          "input_height": 320,
          "classes": ["person", "chair"],
          "confidence_threshold": 0.60,
          "nms_iou_threshold": 0.45,
          "max_detections": 10,
          "checksum": "sha256:abc",
          "model_asset": "yolov8n_320_int8.tflite"
        }
    """.trimIndent()

    @Test
    fun parsesValidManifest() {
        val manifest = ModelManifestJson.parse(validJson).getOrThrow()
        assertEquals("yolov8n-accessibility", manifest.modelId)
        assertEquals(320, manifest.inputWidth)
        assertEquals(listOf("person", "chair"), manifest.classes)
        assertEquals(0.60f, manifest.confidenceThreshold, 1e-6f)
        assertEquals("yolov8n_320_int8.tflite", manifest.modelAsset)
    }

    @Test
    fun rejectsMissingField() {
        val result = ModelManifestJson.parse("""{"model_id":"x"}""")
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsInvalidConfidence() {
        val bad = validJson.replace("0.60", "1.5")
        val result = ModelManifestJson.parse(bad)
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsEmptyClasses() {
        val bad = validJson.replace("""["person", "chair"]""", "[]")
        val result = ModelManifestJson.parse(bad)
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsWrongInputSize() {
        val bad = validJson.replace("320", "9999")
        val result = ModelManifestJson.parse(bad)
        assertTrue(result.isFailure)
    }
}
