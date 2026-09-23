package com.visionrt.inference.manifest

/**
 * Parsed model release manifest (ARCHITECTURE §11.3). Validated at startup
 * before any runtime load; a failed validation must not crash the app
 * (OR-002.7).
 */
data class ModelManifest(
    val modelId: String,
    val version: String,
    val runtime: String,
    val quantization: String,
    val inputWidth: Int,
    val inputHeight: Int,
    val classes: List<String>,
    val confidenceThreshold: Float,
    val nmsIouThreshold: Float,
    val maxDetections: Int,
    val checksum: String,
    /** Asset file name of the LiteRT/TFLite binary next to the manifest. */
    val modelAsset: String,
)

/**
 * Validates raw manifest fields and returns a [ModelManifest] or a failure
 * [Result]. Pure JVM; JSON parsing lives in [ModelManifestJson].
 */
object ModelManifestValidator {

    const val DEFAULT_INPUT = 320
    const val MIN_INPUT = 64
    const val MAX_INPUT = 1024
    private const val BOX_DIMS = 4
    private const val MAX_CHANNELS_PER_HEAD = 512

    /** Collects validation errors; empty list means the raw fields are valid. */
    fun errors(raw: ModelManifest): List<String> {
        val errors = mutableListOf<String>()
        checkBlank(errors, "model_id", raw.modelId)
        checkBlank(errors, "version", raw.version)
        checkBlank(errors, "runtime", raw.runtime)
        checkBlank(errors, "quantization", raw.quantization)
        checkInput(errors, raw)
        checkClasses(errors, raw)
        checkThresholds(errors, raw)
        if (raw.maxDetections < 1) errors += "max_detections < 1"
        checkBlank(errors, "model asset", raw.modelAsset)
        if (raw.classes.size + BOX_DIMS > MAX_CHANNELS_PER_HEAD) {
            errors += "too many classes for detection head"
        }
        return errors
    }

    fun validate(raw: ModelManifest): Result<ModelManifest> {
        val found = errors(raw)
        if (found.isEmpty()) return Result.success(raw)
        return Result.failure(
            IllegalArgumentException("Invalid model manifest: ${found.joinToString("; ")}"),
        )
    }

    private fun checkBlank(errors: MutableList<String>, name: String, value: String) {
        if (value.isBlank()) errors += "$name is blank"
    }

    private fun checkInput(errors: MutableList<String>, raw: ModelManifest) {
        val wOk = raw.inputWidth in MIN_INPUT..MAX_INPUT
        val hOk = raw.inputHeight in MIN_INPUT..MAX_INPUT
        if (!wOk || !hOk) {
            errors += "input ${raw.inputWidth}x${raw.inputHeight} out of range"
        }
    }

    private fun checkClasses(errors: MutableList<String>, raw: ModelManifest) {
        if (raw.classes.isEmpty()) errors += "classes is empty"
        if (raw.classes.any { it.isBlank() }) errors += "blank class label"
    }

    private fun checkThresholds(errors: MutableList<String>, raw: ModelManifest) {
        if (raw.confidenceThreshold <= 0f || raw.confidenceThreshold >= 1f) {
            errors += "confidence_threshold must be in (0,1)"
        }
        if (raw.nmsIouThreshold <= 0f || raw.nmsIouThreshold >= 1f) {
            errors += "nms_iou_threshold must be in (0,1)"
        }
    }
}
