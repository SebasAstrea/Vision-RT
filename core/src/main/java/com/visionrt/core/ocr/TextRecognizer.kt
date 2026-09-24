package com.visionrt.core.ocr

/**
 * Language pack for on-device OCR (OR-002.4). The default pack ships with
 * the APK so core reading works offline; optional packs are opt-in only.
 */
enum class LanguagePack(val assetKey: String) {
    LATIN("latin"),
}

/**
 * Still image handed to [TextRecognizer]. RGB888 row-major, matching
 * [com.visionrt.core.domain] frame buffers so the adapter can convert without
 * an intermediate Bitmap when possible. Text pixels are never logged.
 */
data class TextCapture(
    val width: Int,
    val height: Int,
    val rgb888: ByteArray,
) {
    init {
        require(width > 0 && height > 0) { "TextCapture size must be positive" }
        require(rgb888.size >= width * height * RGB_BYTES_PER_PIXEL) {
            "TextCapture buffer too small for ${width}x$height"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextCapture) return false
        return width == other.width &&
            height == other.height &&
            rgb888.contentEquals(other.rgb888)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + rgb888.contentHashCode()
        return result
    }

    companion object {
        const val RGB_BYTES_PER_PIXEL = 3
    }
}

/** One paragraph / block of recognized text in reading order. */
data class TextBlock(val text: String, val readingOrder: Int)

/** Full OCR result for one capture. Never logged (SafeLog). */
data class RecognizedText(
    val blocks: List<TextBlock>,
    val languageTag: String?,
) {
    val isEmpty: Boolean get() = blocks.isEmpty() || blocks.all { it.text.isBlank() }
}

/**
 * Replaceable OCR port (ARCHITECTURE §11.2, ADR-006). Lazy-load on first use,
 * unload after idle so only one heavy model is resident (OR-002.6–7).
 */
interface TextRecognizer {
    suspend fun load(languagePack: LanguagePack = LanguagePack.LATIN): Result<Unit>
    suspend fun recognize(image: TextCapture): Result<RecognizedText>
    fun isLoaded(): Boolean
    suspend fun unload()
}
