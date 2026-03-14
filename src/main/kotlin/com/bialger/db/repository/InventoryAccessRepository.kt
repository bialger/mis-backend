package com.bialger.db.repository

import com.bialger.db.entity.InventoryAccessEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface InventoryAccessRepository : CrudRepository<InventoryAccessEntity, UUID> {

    fun findByCategoryId(categoryId: UUID): List<InventoryAccessEntity>

    fun findByEmployeeId(employeeId: UUID): List<InventoryAccessEntity>
}
