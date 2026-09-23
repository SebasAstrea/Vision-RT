package com.visionrt.perception

/**
 * Immutable snapshot of one camera frame for the perception → inference handoff.
 *
 * Pixel payload is optional and allocator-light: camera analysis may attach a
 * reused [rgb888] buffer (width×height×3, row-major, no Bitmap). Tests and
 * metadata-only paths leave it null. Equality compares payload by content so
 * data-class tests remain correct.
 */
data class PerceptionFrame(
    val timestampMs: Long,
    val width: Int,
    val height: Int,
    val rgb888: ByteArray? = null,
) {
    init {
        require(width > 0 && height > 0) { "Frame size must be positive" }
        if (rgb888 != null) {
            require(rgb888.size == expectedRgbSize(width, height)) {
                "rgb888 size ${rgb888.size} != width*height*3 ${expectedRgbSize(width, height)}"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerceptionFrame) return false
        if (timestampMs != other.timestampMs) return false
        if (width != other.width) return false
        if (height != other.height) return false
        return rgb888.contentEquals(other.rgb888)
    }

    override fun hashCode(): Int {
        var result = timestampMs.hashCode()
        result = WIDTH_HASH * result + width
        result = WIDTH_HASH * result + height
        result = WIDTH_HASH * result + (rgb888?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        private const val RGB_PLANES = 3
        private const val WIDTH_HASH = 31

        fun expectedRgbSize(width: Int, height: Int): Int = width * height * RGB_PLANES
    }
}
