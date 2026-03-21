package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.BranchEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface BranchRepository : CrudRepository<BranchEntity, UUID> {

    @Query("SELECT * FROM branch ORDER BY name")
    fun findAllOrdered(): List<BranchEntity>

    @Query("SELECT * FROM branch WHERE id IN (:ids)")
    fun findByIds(ids: List<UUID>): List<BranchEntity>

    fun findByOrganizationId(organizationId: UUID): List<BranchEntity>

    fun findByOrganizationIdAndIsActive(organizationId: UUID, isActive: Boolean): List<BranchEntity>
}
