package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("prescription")
data class PrescriptionEntity(
    @Id val id: UUID,
    val medicalRecordId: UUID,
    val type: String,  // PROCEDURE, MEDICATION, LAB_TEST, OTHER
    val description: String,
    val createdAt: Instant? = null
)
