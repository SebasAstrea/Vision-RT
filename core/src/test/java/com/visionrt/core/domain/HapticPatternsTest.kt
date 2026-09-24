package com.visionrt.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPatternsTest {

    @Test
    fun sectorsProduceDistinctRhythms() {
        val left = HapticPatterns.baseRhythm(HorizontalSector.LEFT).toList()
        val center = HapticPatterns.baseRhythm(HorizontalSector.CENTER).toList()
        val right = HapticPatterns.baseRhythm(HorizontalSector.RIGHT).toList()
        assertNotEquals(left, center)
        assertNotEquals(center, right)
        assertNotEquals(left, right)
    }

    @Test
    fun nearIsStrongerThanFar() {
        val near = HapticPatterns.waveform(HorizontalSector.CENTER, Proximity.NEAR).amplitudes.max()
        val far = HapticPatterns.waveform(HorizontalSector.CENTER, Proximity.FAR).amplitudes.max()
        assertTrue("NEAR ($near) must exceed FAR ($far)", near > far)
    }

    @Test
    fun intensityScalesAmplitudeMonotonically() {
        val light = HapticPatterns
            .waveform(HorizontalSector.CENTER, Proximity.NEAR, HapticIntensity.LIGHT)
            .amplitudes
            .max()
        val medium = HapticPatterns
            .waveform(HorizontalSector.CENTER, Proximity.NEAR, HapticIntensity.MEDIUM)
            .amplitudes
            .max()
        val strong = HapticPatterns
            .waveform(HorizontalSector.CENTER, Proximity.NEAR, HapticIntensity.STRONG)
            .amplitudes
            .max()
        assertTrue(light < medium)
        assertTrue(medium < strong)
        assertEquals(255, strong)
    }

    @Test
    fun waveformLengthsMatchAndStayInRange() {
        HorizontalSector.entries.forEach { sector ->
            Proximity.entries.forEach { proximity ->
                val wave = HapticPatterns.waveform(sector, proximity, HapticIntensity.STRONG)
                assertEquals(wave.timings.size, wave.amplitudes.size)
                assertTrue(wave.timings.all { it >= 0 })
                assertTrue(wave.amplitudes.all { it in 0..255 })
            }
        }
    }

    @Test
    fun earconMapsEverySector() {
        assertEquals(EarconType.LEFT, HapticPatterns.earconFor(HorizontalSector.LEFT))
        assertEquals(EarconType.CENTER, HapticPatterns.earconFor(HorizontalSector.CENTER))
        assertEquals(EarconType.RIGHT, HapticPatterns.earconFor(HorizontalSector.RIGHT))
        assertEquals(EarconType.CENTER, HapticPatterns.earconFor(null))
    }

    @Test
    fun nearGapsAreTighterThanFar() {
        val near = HapticPatterns.waveform(HorizontalSector.LEFT, Proximity.NEAR)
        val far = HapticPatterns.waveform(HorizontalSector.LEFT, Proximity.FAR)
        // First gap is the delay before the first pulse pair (index 2 in rhythm).
        assertTrue(near.timings[2] < far.timings[2])
    }
}
