package com.visionrt.inference.postprocess

import com.visionrt.core.domain.BoundingBox
import com.visionrt.core.domain.Detection
import com.visionrt.core.domain.HorizontalSector
import com.visionrt.core.domain.Proximity
import com.visionrt.inference.manifest.ModelManifest
import kotlin.math.max
import kotlin.math.min

/**
 * YOLOv8-style tensor decoding: confidence filter, greedy NMS, sector and
 * proximity approximation (ARCHITECTURE §10.1, §11; M3 deliverable 7).
 *
 * Accepts channel-first output `[1, 4+numClasses, numAnchors]` with box
 * values in input-pixel space (cx, cy, w, h). Pure JVM, unit-testable.
 */
object DetectionPostProcessor {

    /** Fraction of frame area at/above which a box counts as NEAR. */
    const val NEAR_AREA_FRACTION = 0.20f

    /** Fraction of frame area at/above which a box counts as MEDIUM. */
    const val MEDIUM_AREA_FRACTION = 0.05f

    /** Horizontal sector split points on normalized center-x. */
    const val LEFT_MAX_CX = 1f / 3f
    const val RIGHT_MIN_CX = 2f / 3f

    private const val BOX_DIMS = 4
    private const val MIN_BOX_SIZE = 1e-4f
    private const val HALF = 2f
    private const val CX = 0
    private const val CY = 1
    private const val W = 2
    private const val H = 3
    private const val UNIT_W = 1f

    private data class Size(
        val width: Float,
        val height: Float,
    )

    private data class RawBox(
        val cx: Float,
        val cy: Float,
        val w: Float,
        val h: Float,
    )

    /**
     * @param output flat row-major of shape [channels, anchors] where
     *   channels = 4 box + classes.size scores (omitting the outer batch dim).
     */
    fun process(
        output: FloatArray,
        channels: Int,
        anchors: Int,
        manifest: ModelManifest,
        timestampMs: Long,
    ): List<Detection> {
        val numClasses = manifest.classes.size
        require(channels == BOX_DIMS + numClasses) {
            "channels $channels != 4+classes ${BOX_DIMS + numClasses}"
        }
        require(anchors >= 1 && output.size == channels * anchors) {
            "output size ${output.size} != channels*anchors ${channels * anchors}"
        }
        val candidates = collectCandidates(output, anchors, manifest)
        val kept = nms(candidates, manifest.nmsIouThreshold, manifest.maxDetections)
        return kept.mapIndexed { index, c -> toDetection(c, index, manifest, timestampMs) }
    }

    fun sectorOf(box: BoundingBox): HorizontalSector {
        val cx = (box.left + box.right) / HALF
        return when {
            cx < LEFT_MAX_CX -> HorizontalSector.LEFT
            cx > RIGHT_MIN_CX -> HorizontalSector.RIGHT
            else -> HorizontalSector.CENTER
        }
    }

    fun proximityOf(box: BoundingBox): Proximity {
        val area = (box.right - box.left) * (box.bottom - box.top)
        return when {
            area >= NEAR_AREA_FRACTION -> Proximity.NEAR
            area >= MEDIUM_AREA_FRACTION -> Proximity.MEDIUM
            area > 0f -> Proximity.FAR
            else -> Proximity.UNKNOWN
        }
    }

    private fun toDetection(
        c: Candidate,
        index: Int,
        manifest: ModelManifest,
        timestampMs: Long,
    ): Detection {
        val box = BoundingBox(
            left = c.left,
            top = c.top,
            right = max(c.right, c.left + MIN_BOX_SIZE),
            bottom = max(c.bottom, c.top + MIN_BOX_SIZE),
        )
        return Detection(
            id = "det-$timestampMs-$index",
            classId = c.classId,
            label = manifest.classes[c.classId],
            confidence = c.score,
            boundingBox = box,
            sector = sectorOf(box),
            proximity = proximityOf(box),
            timestampMs = timestampMs,
            sourceModelVersion = "${manifest.modelId}@${manifest.version}",
        )
    }

    private fun collectCandidates(
        output: FloatArray,
        anchors: Int,
        manifest: ModelManifest,
    ): List<Candidate> {
        val size = Size(manifest.inputWidth.toFloat(), manifest.inputHeight.toFloat())
        val candidates = mutableListOf<Candidate>()
        for (a in 0 until anchors) {
            bestClass(output, anchors, a, manifest)
                ?.let { (classId, score) ->
                    val box = RawBox(
                        cx = output[CX * anchors + a],
                        cy = output[CY * anchors + a],
                        w = output[W * anchors + a],
                        h = output[H * anchors + a],
                    )
                    candidateOf(box, classId, score, size)
                        ?.let { candidates += it }
                }
        }
        return candidates
    }

    private fun candidateOf(
        box: RawBox,
        classId: Int,
        score: Float,
        size: Size,
    ): Candidate? {
        val norm = if (box.w > 0f && box.h > 0f) {
            normalizedBox(box.cx, box.cy, box.w, box.h, size)
        } else {
            null
        }
        return norm?.let { Candidate(classId, score, it.first, it.second, it.third, it.fourth) }
    }

    private fun bestClass(
        output: FloatArray,
        anchors: Int,
        anchor: Int,
        manifest: ModelManifest,
    ): Pair<Int, Float>? {
        var bestClass = -1
        var bestScore = 0f
        for (c in 0 until manifest.classes.size) {
            val score = output[(BOX_DIMS + c) * anchors + anchor]
            if (score > bestScore) {
                bestScore = score
                bestClass = c
            }
        }
        val ok = bestClass >= 0 && bestScore >= manifest.confidenceThreshold
        return if (ok) bestClass to bestScore else null
    }

    private data class BoxNorm(
        val first: Float,
        val second: Float,
        val third: Float,
        val fourth: Float,
    )

    private fun normalizedBox(
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        size: Size,
    ): BoxNorm? {
        val left = (cx - w / HALF) / size.width
        val top = (cy - h / HALF) / size.height
        val right = (cx + w / HALF) / size.width
        val bottom = (cy + h / HALF) / size.height
        val inside = right > left && bottom > top && right > 0f && bottom > 0f &&
            left < UNIT_W && top < UNIT_W
        return if (inside) {
            BoxNorm(
                left.coerceIn(0f, UNIT_W),
                top.coerceIn(0f, UNIT_W),
                right.coerceIn(0f, UNIT_W),
                bottom.coerceIn(0f, UNIT_W),
            )
        } else {
            null
        }
    }

    internal data class Candidate(
        val classId: Int,
        val score: Float,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    )

    internal fun nms(
        candidates: List<Candidate>,
        iouThreshold: Float,
        maxDetections: Int,
    ): List<Candidate> {
        val selected = mutableListOf<Candidate>()
        for (cand in candidates.sortedByDescending { it.score }) {
            if (selected.size >= maxDetections) break
            val overlaps = selected.any { iou(it, cand) > iouThreshold }
            if (!overlaps) selected += cand
        }
        return selected
    }

    internal fun iou(a: Candidate, b: Candidate): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)
        val interW = interRight - interLeft
        val interH = interBottom - interTop
        if (interW <= 0f || interH <= 0f) return 0f
        val inter = interW * interH
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        return inter / (areaA + areaB - inter)
    }
}
