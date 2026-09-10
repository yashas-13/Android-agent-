package com.example.agent

enum class DeliveryStatus {
    PENDING,
    SENDING,
    SUCCESS,
    FAILED
}

data class GroupDeliveryResult(
    val groupName: String,
    val status: DeliveryStatus,
    val failureReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
