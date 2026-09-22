package com.visionrt.core.domain

/**
 * Core assistance modes (FR-003). First-class domain values used by the
 * orchestration layer (M2+) and by the UI shell (M1) for navigation.
 */
enum class AssistanceMode(val key: String) {
    OBSTACLE_AWARENESS("obstacle_awareness"),
    OBJECT_SUMMARY("object_summary"),
    TEXT_READING("text_reading"),
    SETTINGS("settings"),
    HELP("help");
}
