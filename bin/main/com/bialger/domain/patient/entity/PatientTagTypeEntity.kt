package com.bialger.domain.patient.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("patient_tag_type")
data class PatientTagTypeEntity(
    @Id val id: UUID,
    val code: String,
    val icon: String? = null,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true
)
