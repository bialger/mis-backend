package com.bialger.db.entity

import com.bialger.db.converter.ConsentTypeConverter
import com.bialger.db.enums.ConsentType
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("patient_consent")
data class PatientConsentEntity(
    @Id val id: UUID,
    val patientId: UUID,
    @field:TypeDef(type = DataType.OBJECT, converter = ConsentTypeConverter::class)
    val consentType: ConsentType,
    val isGranted: Boolean,
    val grantedAt: Instant? = null,
    val revokedAt: Instant? = null
)
