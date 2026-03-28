package com.bialger.domain.inventory.repository

import com.bialger.domain.inventory.entity.InventoryItemEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface InventoryItemRepository : CrudRepository<InventoryItemEntity, UUID> {

    @Query("SELECT * FROM inventory_item ORDER BY name")
    fun findAllOrdered(): List<InventoryItemEntity>

    fun findByBranchId(branchId: UUID): List<InventoryItemEntity>

    fun findByCategoryId(categoryId: UUID): List<InventoryItemEntity>
}
