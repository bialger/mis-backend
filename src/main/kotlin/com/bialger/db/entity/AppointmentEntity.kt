package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("appointment")
data class AppointmentEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val employeeId: UUID,
    val timeSlotId: UUID? = null,
    val branchId: UUID,
    val roomId: UUID,
    val status: String,  // appointment_status enum
    val source: String,  // appointment_source enum
    val notes: String? = null,
    val createdBy: UUID? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
