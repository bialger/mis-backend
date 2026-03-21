package com.bialger.domain.inventory.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity("inventory_category")
data class InventoryCategoryEntity(
    @Id val id: java.util.UUID,
    val name: String,
    val description: String? = null
)
