package com.bialger.domain.inventory.repository

import com.bialger.domain.inventory.entity.InventoryCategoryEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface InventoryCategoryRepository : CrudRepository<InventoryCategoryEntity, UUID> {

    @Query("SELECT * FROM inventory_category ORDER BY name")
    fun findAllOrdered(): List<InventoryCategoryEntity>

    fun findByName(name: String): InventoryCategoryEntity?
}
