package com.visionrt.inference.runtimeapi

import com.visionrt.core.domain.Detection
import com.visionrt.perception.PerceptionFrame

/**
 * Immutable model definition for the YOLOv8n INT8 export (ARCHITECTURE §11/§14).
 * Every runtime instance is bound to exactly one [ModelConfig]; changing config
 * means unload + load.
 */
data class ModelConfig(
    val modelId: String,
    val modelVersion: String,
    val runtime: String,
    val quantization: String,
    val inputWidth: Int,
    val inputHeight: Int,
    val confidenceThreshold: Float,
    val nmsIouThreshold: Float,
    val maxDetections: Int,
)

/** Which runtime provides a [ModelConfig]; injected by the app in M3+. */
enum class InferenceVendor { LITERT, ONNX_OPTIONAL }

/**
 * Object detector abstraction owned by the app. No module may talk to LiteRT
 * (or ONNX) directly except the implementations of these interfaces.
 */
interface ObjectDetector {
    suspend fun load(config: ModelConfig): Result<Unit>
    suspend fun detect(frame: PerceptionFrame): Result<List<Detection>>
    fun isLoaded(): Boolean
    suspend fun unload()
}
