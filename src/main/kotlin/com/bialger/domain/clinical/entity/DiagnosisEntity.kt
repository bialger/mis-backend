package com.bialger.domain.clinical.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("diagnosis")
data class DiagnosisEntity(
    @Id val id: UUID,
    val medicalRecordId: UUID,
    val code: String? = null,
    val name: String,
    val description: String? = null,
    val isPrimary: Boolean = false
)
