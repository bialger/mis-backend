package com.bialger.domain.patient.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("patient_tag")
data class PatientTagEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val tagTypeId: UUID,
    val createdBy: UUID? = null,
    val createdAt: Instant? = null
)
