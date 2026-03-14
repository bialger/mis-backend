package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("notification")
data class NotificationEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val appointmentId: UUID? = null,
    val channel: String,  // SMS, EMAIL
    val type: String,     // APPOINTMENT_CONFIRMATION, VISIT_REMINDER, MARKETING
    val content: String? = null,
    val status: String,   // PENDING, SENT, FAILED
    val sentAt: Instant? = null,
    val createdAt: Instant? = null
)
