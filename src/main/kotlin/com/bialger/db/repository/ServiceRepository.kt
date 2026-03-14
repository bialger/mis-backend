package com.bialger.db.repository

import com.bialger.db.entity.ServiceEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ServiceRepository : CrudRepository<ServiceEntity, UUID> {

    fun findByBranchId(branchId: UUID): List<ServiceEntity>

    fun findByIsActive(isActive: Boolean): List<ServiceEntity>
}
