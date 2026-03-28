package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.OrganizationEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface OrganizationRepository : CrudRepository<OrganizationEntity, UUID> {

    @Query("SELECT * FROM organization ORDER BY name")
    fun findAllOrdered(): List<OrganizationEntity>

    @Query("SELECT * FROM organization WHERE id IN (:ids)")
    fun findByIds(ids: List<UUID>): List<OrganizationEntity>

    fun findByName(name: String): OrganizationEntity?
}
