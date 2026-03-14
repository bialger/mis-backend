package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("attachment")
data class AttachmentEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val appointmentId: UUID? = null,
    val medicalRecordId: UUID? = null,
    val fileName: String,
    val fileType: String,           // PDF, IMAGE, TEXT
    val filePath: String,
    val fileSize: Long? = null,
    val description: String? = null,
    val uploadedBy: UUID,
    val uploadedAt: Instant? = null
)
