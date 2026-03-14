package com.bialger.db.entity

import com.bialger.db.converter.GenderTypeConverter
import com.bialger.db.converter.LocalityTypeConverter
import com.bialger.db.enums.GenderType
import com.bialger.db.enums.LocalityType
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@MappedEntity("patient")
data class PatientEntity(
    @Id val id: UUID,
    val cardNumber: String,
    val organizationId: UUID,
    val fullName: String,
    @field:TypeDef(type = DataType.OBJECT, converter = GenderTypeConverter::class)
    val gender: GenderType? = null,
    val birthDate: LocalDate? = null,
    val registrationAddress: String? = null,
    val residenceAddress: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @field:TypeDef(type = DataType.OBJECT, converter = LocalityTypeConverter::class)
    val localityType: LocalityType? = null,
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
