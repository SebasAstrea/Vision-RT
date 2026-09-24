package com.visionrt.core.domain

/**
 * Non-speech earcon keyed by horizontal sector (FR-010, UX-005).
 * Concrete SoundPool assets live in the `feedback` module.
 */
enum class EarconType { LEFT, CENTER, RIGHT }

/**
 * Haptic urgency for a proximity band. Direction is encoded separately by the
 * pulse rhythm (FR-011.2–3): left/center/right must feel different, and near
 * must feel stronger or more urgent than far.
 */
enum class HapticIntensity { LIGHT, MEDIUM, STRONG }

/**
 * Platform-agnostic vibration waveform (timings alternate delay/vibrate like
 * [android.os.VibrationEffect.createWaveform]; amplitudes are 0..255).
 * Pure data so unit tests never touch the Android vibrator.
 */
data class HapticWaveform(
    val timings: LongArray,
    val amplitudes: IntArray,
) {
    init {
        require(timings.size == amplitudes.size) {
            "Waveform timing/amplitude length mismatch: ${timings.size} vs ${amplitudes.size}"
        }
        require(timings.isNotEmpty()) { "Waveform must not be empty" }
        require(timings.all { it >= 0 }) { "Waveform timings must be non-negative" }
        require(amplitudes.all { it in MIN_AMPLITUDE..MAX_AMPLITUDE }) {
            "Waveform amplitudes must be $MIN_AMPLITUDE..$MAX_AMPLITUDE"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HapticWaveform) return false
        return timings.contentEquals(other.timings) && amplitudes.contentEquals(other.amplitudes)
    }

    override fun hashCode(): Int {
        var result = timings.contentHashCode()
        result = 31 * result + amplitudes.contentHashCode()
        return result
    }

    private companion object {
        const val MIN_AMPLITUDE = 0
        const val MAX_AMPLITUDE = 255
    }
}

/**
 * Pure haptic pattern matrix for FR-011 / UX-005: distinct rhythms per
 * [HorizontalSector], scaled urgency per [Proximity] and user
 * [HapticIntensity]. The feedback module maps the result to VibrationEffect.
 */
@Suppress("MagicNumber") // FR-011 pattern timings/amplitudes are domain data, not logic.
object HapticPatterns {

    // Rhythm templates: [delay0, pulse0, delay1, pulse1, …]
    private val LEFT_RHYTHM = longArrayOf(0, 60, 80, 60)
    private val CENTER_RHYTHM = longArrayOf(0, 180)
    private val RIGHT_RHYTHM = longArrayOf(0, 50, 50, 50, 50, 50)
    private val FALLBACK_RHYTHM = longArrayOf(0, 120)

    private const val AMP_NEAR = 255
    private const val AMP_MEDIUM = 170
    private const val AMP_FAR = 100
    private const val AMP_UNKNOWN = 140
    private const val MIN_AMPLITUDE = 0
    private const val MAX_AMPLITUDE = 255
    private const val EVEN_INDEX = 2

    private const val SCALE_LIGHT = 0.45f
    private const val SCALE_MEDIUM = 0.75f
    private const val SCALE_STRONG = 1.0f

    private const val GAP_NEAR = 0.6
    private const val GAP_MEDIUM = 1.0
    private const val GAP_FAR = 1.4

    /** Base rhythm (delay, vibrate, …) per sector before proximity/intensity. */
    fun baseRhythm(sector: HorizontalSector?): LongArray = when (sector) {
        HorizontalSector.LEFT -> LEFT_RHYTHM
        HorizontalSector.CENTER -> CENTER_RHYTHM
        HorizontalSector.RIGHT -> RIGHT_RHYTHM
        null -> FALLBACK_RHYTHM
    }

    /** Peak amplitude before user intensity scaling (0..255). */
    fun proximityAmplitude(proximity: Proximity?): Int = when (proximity) {
        Proximity.NEAR -> AMP_NEAR
        Proximity.MEDIUM -> AMP_MEDIUM
        Proximity.FAR -> AMP_FAR
        Proximity.UNKNOWN -> AMP_UNKNOWN
        null -> AMP_UNKNOWN
    }

    /** Multiplier applied to every non-zero amplitude (FR-011.5). */
    fun intensityScale(intensity: HapticIntensity): Float = when (intensity) {
        HapticIntensity.LIGHT -> SCALE_LIGHT
        HapticIntensity.MEDIUM -> SCALE_MEDIUM
        HapticIntensity.STRONG -> SCALE_STRONG
    }

    /**
     * Builds the full waveform for one alert. Near also shortens inter-pulse
     * gaps so it feels more urgent than far (FR-011.3).
     */
    fun waveform(
        sector: HorizontalSector?,
        proximity: Proximity?,
        intensity: HapticIntensity = HapticIntensity.MEDIUM,
    ): HapticWaveform {
        val rhythm = baseRhythm(sector)
        val peak = (proximityAmplitude(proximity) * intensityScale(intensity))
            .toInt()
            .coerceIn(MIN_AMPLITUDE + 1, MAX_AMPLITUDE)
        val gapScale = when (proximity) {
            Proximity.NEAR -> GAP_NEAR
            Proximity.MEDIUM -> GAP_MEDIUM
            Proximity.FAR -> GAP_FAR
            else -> GAP_MEDIUM
        }
        val amplitudes = IntArray(rhythm.size) { index ->
            // Even indices are delays (amplitude 0); odd indices vibrate.
            if (index % EVEN_INDEX == 0) MIN_AMPLITUDE else peak
        }
        val timings = LongArray(rhythm.size) { index ->
            if (index % EVEN_INDEX == 0) {
                (rhythm[index] * gapScale).toLong().coerceAtLeast(0L)
            } else {
                rhythm[index]
            }
        }
        return HapticWaveform(timings = timings, amplitudes = amplitudes)
    }

    fun earconFor(sector: HorizontalSector?): EarconType = when (sector) {
        HorizontalSector.LEFT -> EarconType.LEFT
        HorizontalSector.RIGHT -> EarconType.RIGHT
        HorizontalSector.CENTER, null -> EarconType.CENTER
    }
}
