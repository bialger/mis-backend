package com.bialger.domain.inventory.repository

import com.bialger.domain.inventory.entity.ServiceInventoryItemEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ServiceInventoryItemRepository : CrudRepository<ServiceInventoryItemEntity, UUID> {

    @Query("SELECT * FROM service_inventory_item WHERE service_id = :serviceId")
    fun findByServiceId(serviceId: UUID): List<ServiceInventoryItemEntity>

    @Query("SELECT * FROM service_inventory_item WHERE inventory_item_id = :inventoryItemId")
    fun findByInventoryItemId(inventoryItemId: UUID): List<ServiceInventoryItemEntity>
}
