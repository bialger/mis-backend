package com.bialger.db.repository

import com.bialger.db.entity.IntegrationEntity
import com.bialger.db.enums.IntegrationType
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface IntegrationRepository : CrudRepository<IntegrationEntity, UUID> {

    fun findByType(type: IntegrationType): List<IntegrationEntity>

    fun findByIsActive(isActive: Boolean): List<IntegrationEntity>

    fun findByTypeAndIsActive(type: IntegrationType, isActive: Boolean): List<IntegrationEntity>
}
