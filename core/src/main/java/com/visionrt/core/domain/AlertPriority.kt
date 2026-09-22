package com.visionrt.core.domain

/** Voice+earcon dispatch priority, strictly ordered (ARCHITECTURE §9.1). */
enum class AlertPriority { CRITICAL_OBSTACLE, USER_REQUESTED, OCR_RESULT, STATUS, DEBUG }
