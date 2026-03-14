package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@MappedEntity("patient")
data class PatientEntity(
    @Id val id: UUID,
    val cardNumber: String,
    val organizationId: UUID,
    val fullName: String,
    val gender: String? = null,        // M, F - PostgreSQL enum
    val birthDate: LocalDate? = null,
    val registrationAddress: String? = null,
    val residenceAddress: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val localityType: String? = null,  // URBAN, RURAL - PostgreSQL enum
    val citizenship: String? = null,
    val identityDocument: String? = null,
    val omsPolicy: String? = null,
    val snils: String? = null,
    val insuranceOrganization: String? = null,
    val contactPerson: String? = null,
    val guardian: String? = null,
    val profession: String? = null,
    val workplace: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
