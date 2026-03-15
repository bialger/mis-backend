package com.bialger.domain.inventory.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity("inventory_item")
data class InventoryItemEntity(
    @Id val id: UUID,
    val categoryId: UUID,
    val branchId: UUID,
    val roomId: UUID? = null,
    val name: String,
    val unit: String? = null,
    val quantity: BigDecimal = BigDecimal.ZERO,
    val minQuantity: BigDecimal? = null,
    val costPrice: BigDecimal? = null
)
