package com.bialger.domain.inventory.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("inventory_access")
data class InventoryAccessEntity(
    @Id val id: UUID,
    val employeeId: UUID? = null,
    val roleId: UUID? = null,
    val categoryId: UUID
)
