package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Пациент")
data class PatientRestDto(
    val id: String,
    val organizationId: String,
    val cardNumber: String,
    val fullName: String,
    val gender: String?,
    val birthDate: String?,
    val phone: String?,
    val email: String?,
    val registrationAddress: String?,
    val residenceAddress: String?,
    val localityType: String?,
    val citizenship: String?,
    val identityDocument: String?,
    val omsPolicy: String?,
    val snils: String?,
    val insuranceOrganization: String?,
    val contactPerson: String?,
    val guardian: String?,
    val profession: String?,
    val workplace: String?
)

@Serdeable
@Introspected
@Schema(description = "Создание пациента")
data class PatientCreateDto(
    @field:NotNull @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val organizationId: UUID,
    @field:NotBlank val cardNumber: String,
    @field:NotBlank val fullName: String,
    val gender: String? = null,
    val birthDate: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val registrationAddress: String? = null,
    val residenceAddress: String? = null,
    val localityType: String? = null,
    val citizenship: String? = null,
    val identityDocument: String? = null,
    val omsPolicy: String? = null,
    val snils: String? = null,
    val insuranceOrganization: String? = null,
    val contactPerson: String? = null,
    val guardian: String? = null,
    val profession: String? = null,
    val workplace: String? = null
)

@Serdeable
@Introspected
@Schema(description = "Обновление пациента")
data class PatientUpdateDto(
    @field:NotNull @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val organizationId: UUID,
    @field:NotBlank val cardNumber: String,
    @field:NotBlank val fullName: String,
    val gender: String? = null,
    val birthDate: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val registrationAddress: String? = null,
    val residenceAddress: String? = null,
    val localityType: String? = null,
    val citizenship: String? = null,
    val identityDocument: String? = null,
    val omsPolicy: String? = null,
    val snils: String? = null,
    val insuranceOrganization: String? = null,
    val contactPerson: String? = null,
    val guardian: String? = null,
    val profession: String? = null,
    val workplace: String? = null
)
