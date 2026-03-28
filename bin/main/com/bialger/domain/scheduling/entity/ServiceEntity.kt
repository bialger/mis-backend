package com.bialger.domain.scheduling.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity("service")
data class ServiceEntity(
    @Id val id: UUID,
    val name: String,
    val price: BigDecimal,
    val costPrice: BigDecimal? = null,
    val branchId: UUID? = null,
    val isActive: Boolean = true
)
