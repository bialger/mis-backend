package com.bialger.domain.clinical.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("medical_record")
data class MedicalRecordEntity(
    @Id val id: UUID,
    val appointmentId: UUID,
    val patientId: UUID,
    val employeeId: UUID,
    val complaints: String? = null,
    val anamnesis: String? = null,
    val examinationResults: String? = null,
    val diseaseCourse: String? = null,
    val procedures: String? = null,
    val epicrisis: String? = null,
    val templateId: UUID? = null,
    val isSigned: Boolean = false,
    val signatureData: ByteArray? = null,
    val signedAt: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MedicalRecordEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
