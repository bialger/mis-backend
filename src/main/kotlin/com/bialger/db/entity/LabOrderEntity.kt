package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@MappedEntity("lab_order")
data class LabOrderEntity(
    @Id val id: UUID,
    val medicalRecordId: UUID,
    val patientId: UUID,
    val employeeId: UUID,
    val totalPrice: BigDecimal? = null,
    val status: String,
    val createdAt: Instant? = null
)
