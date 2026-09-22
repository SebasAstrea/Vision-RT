package com.visionrt.core.domain

data class Alert(
    val id: String,
    val priority: AlertPriority,
    val message: String,
    val createdAtMs: Long,
)
