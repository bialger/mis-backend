package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Introspected
@Schema(description = "Role reference")
data class RoleRestDto(
    val id: String,
    val name: String,
    val displayName: String?
)

@Serdeable
@Introspected
@Schema(description = "Attachment metadata response")
data class AttachmentRestDto(
    val id: String,
    val patientId: String,
    val appointmentId: String?,
    val medicalRecordId: String?,
    val fileName: String,
    val fileType: String,
    val filePath: String,
    val fileSize: Long?,
    val description: String?,
    val uploadedBy: String,
    val uploadedAt: String?
)
