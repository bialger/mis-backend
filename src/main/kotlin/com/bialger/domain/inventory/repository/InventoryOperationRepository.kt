package com.bialger.domain.inventory.repository

import com.bialger.domain.inventory.entity.InventoryOperationEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface InventoryOperationRepository : CrudRepository<InventoryOperationEntity, UUID> {

    @Query("SELECT * FROM inventory_operation WHERE item_id = :itemId ORDER BY created_at DESC NULLS LAST")
    fun findByItemIdOrdered(itemId: UUID): List<InventoryOperationEntity>

    @Query("SELECT * FROM inventory_operation ORDER BY created_at DESC NULLS LAST")
    fun findAllOrdered(): List<InventoryOperationEntity>

    fun findByItemId(itemId: UUID): List<InventoryOperationEntity>

    fun findByEmployeeId(employeeId: UUID): List<InventoryOperationEntity>
}
