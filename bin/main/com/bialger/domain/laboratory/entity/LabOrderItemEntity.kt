package com.bialger.domain.laboratory.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity("lab_order_item")
data class LabOrderItemEntity(
    @Id val id: UUID,
    val labOrderId: UUID,
    val labTestId: UUID,
    val price: BigDecimal? = null
)
