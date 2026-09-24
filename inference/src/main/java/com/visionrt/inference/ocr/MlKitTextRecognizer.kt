package com.visionrt.inference.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.visionrt.core.ocr.LanguagePack
import com.visionrt.core.ocr.RecognizedText
import com.visionrt.core.ocr.TextBlock
import com.visionrt.core.ocr.TextCapture
import com.visionrt.core.ocr.TextRecognizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * ML Kit Text Recognition v2 (ADR-006, ARCHITECTURE §6.8 / §11.2).
 *
 * Lazy [load] holds the ML Kit client; [unload] closes it so the native
 * OCR model is not resident while the detector runs (OR-002.6). Latin pack
 * is built into the dependency — offline after install (FR-009.2).
 */
@Singleton
class MlKitTextRecognizer @Inject constructor() : TextRecognizer {

    private var client: com.google.mlkit.vision.text.TextRecognizer? = null

    @Volatile
    private var loaded: Boolean = false

    override suspend fun load(languagePack: LanguagePack): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                if (loaded && client != null) return@runCatching
                // Latin script recognizer covers the default offline pack (OR-002.4).
                client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                loaded = true
            }
        }

    override suspend fun recognize(image: TextCapture): Result<RecognizedText> =
        withContext(Dispatchers.Default) {
            runCatching {
                val active = client ?: error("OCR not loaded")
                val bitmap = rgb888ToBitmap(image)
                val visionText = active.process(InputImage.fromBitmap(bitmap, 0)).await()
                bitmap.recycle()
                val sorted = visionText.textBlocks.sortedByDescending { block ->
                    block.boundingBox?.height() ?: 0
                }
                val blocks = sorted
                    .mapIndexed { index, block ->
                        TextBlock(text = block.text.trim(), readingOrder = index)
                    }
                    .filter { it.text.isNotBlank() }
                RecognizedText(blocks = blocks, languageTag = null)
            }
        }

    override fun isLoaded(): Boolean = loaded

    override suspend fun unload() {
        withContext(Dispatchers.Default) {
            client?.close()
            client = null
            loaded = false
        }
    }

    private fun rgb888ToBitmap(capture: TextCapture): Bitmap {
        val pixels = IntArray(capture.width * capture.height)
        val rgb = capture.rgb888
        var i = 0
        var p = 0
        while (p < pixels.size && i + RGB_LAST < rgb.size) {
            val r = rgb[i].toInt() and BYTE_MASK
            val g = rgb[i + G_OFFSET].toInt() and BYTE_MASK
            val b = rgb[i + B_OFFSET].toInt() and BYTE_MASK
            pixels[p] = (FULL_OPAQUE or (r shl R_SHIFT) or (g shl G_SHIFT) or (b shl B_SHIFT))
            i += RGB_BYTES
            p++
        }
        return Bitmap.createBitmap(pixels, capture.width, capture.height, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val RGB_BYTES = 3
        const val RGB_LAST = 2
        const val G_OFFSET = 1
        const val B_OFFSET = 2
        const val BYTE_MASK = 0xFF
        const val FULL_OPAQUE = 0xFF000000.toInt()
        const val R_SHIFT = 16
        const val G_SHIFT = 8
        const val B_SHIFT = 0
    }
}
