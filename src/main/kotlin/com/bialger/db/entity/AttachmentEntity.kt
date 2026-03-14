package com.bialger.db.entity

import com.bialger.db.converter.FileTypeConverter
import com.bialger.db.enums.FileType
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("attachment")
data class AttachmentEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val appointmentId: UUID? = null,
    val medicalRecordId: UUID? = null,
    val fileName: String,
    @field:TypeDef(type = DataType.OBJECT, converter = FileTypeConverter::class)
    val fileType: FileType,
    val filePath: String,
    val fileSize: Long? = null,
    val description: String? = null,
    val uploadedBy: UUID,
    val uploadedAt: Instant? = null
)
