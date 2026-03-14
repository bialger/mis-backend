package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity("lab_test")
data class LabTestEntity(
    @Id val id: UUID,
    val name: String,
    val description: String? = null,
    val price: BigDecimal? = null,
    val laboratoryId: UUID? = null,
    val isActive: Boolean = true
)
