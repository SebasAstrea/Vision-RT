package com.visionrt.inference.manifest

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses ARCHITECTURE §11.3 manifest JSON into a validated [ModelManifest].
 * Uses org.json (bundled on Android; test classpath provides the JVM artifact).
 */
object ModelManifestJson {

    fun parse(json: String): Result<ModelManifest> = runCatching {
        val root = JSONObject(json)
        val classesJson = root.getJSONArray("classes")
        val classes = buildList {
            for (i in 0 until classesJson.length()) {
                add(classesJson.getString(i))
            }
        }
        val raw = ModelManifest(
            modelId = root.getString("model_id"),
            version = root.getString("version"),
            runtime = root.getString("runtime"),
            quantization = root.getString("quantization"),
            inputWidth = root.getInt("input_width"),
            inputHeight = root.getInt("input_height"),
            classes = classes,
            confidenceThreshold = root.getDouble("confidence_threshold").toFloat(),
            nmsIouThreshold = root.getDouble("nms_iou_threshold").toFloat(),
            maxDetections = root.getInt("max_detections"),
            checksum = root.optString("checksum", ""),
            modelAsset = root.optString("model_asset", ""),
        )
        ModelManifestValidator.validate(raw).getOrThrow()
    }
}
