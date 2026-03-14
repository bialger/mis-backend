package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("insert_sheet")
data class InsertSheetEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val appointmentId: UUID,
    val employeeId: UUID,
    val formType: String,
    val content: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
