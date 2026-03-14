package com.bialger.db.repository

import com.bialger.db.entity.InventoryOperationEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface InventoryOperationRepository : CrudRepository<InventoryOperationEntity, UUID> {

    fun findByItemId(itemId: UUID): List<InventoryOperationEntity>

    fun findByEmployeeId(employeeId: UUID): List<InventoryOperationEntity>
}
