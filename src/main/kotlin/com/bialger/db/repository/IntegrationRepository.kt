package com.bialger.db.repository

import com.bialger.db.entity.IntegrationEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface IntegrationRepository : CrudRepository<IntegrationEntity, UUID> {

    fun findByType(type: String): List<IntegrationEntity>

    fun findByIsActive(isActive: Boolean): List<IntegrationEntity>

    fun findByTypeAndIsActive(type: String, isActive: Boolean): List<IntegrationEntity>
}
