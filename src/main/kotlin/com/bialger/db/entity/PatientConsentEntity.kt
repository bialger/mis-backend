package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("patient_consent")
data class PatientConsentEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val consentType: String,  // GOV_DATA_TRANSFER, MARKETING
    val isGranted: Boolean,
    val grantedAt: Instant? = null,
    val revokedAt: Instant? = null
)
