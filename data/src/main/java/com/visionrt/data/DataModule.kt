package com.visionrt.data

/**
 * Data module marker. Sub-packages (M2+): settings, telemetry, diagnostics.
 * Persistence must stay inside this module; nobody reads/writes settings or
 * telemetry stores directly.
 */
object DataModule
