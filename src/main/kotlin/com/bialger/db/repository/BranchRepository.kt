package com.bialger.db.repository

import com.bialger.db.entity.BranchEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface BranchRepository : CrudRepository<BranchEntity, UUID> {

    fun findByOrganizationId(organizationId: UUID): List<BranchEntity>

    fun findByOrganizationIdAndIsActive(organizationId: UUID, isActive: Boolean): List<BranchEntity>
}
