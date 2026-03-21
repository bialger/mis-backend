package com.bialger.domain.attachment.repository

import com.bialger.domain.attachment.entity.IntegrationEntity
import com.bialger.domain.attachment.enums.IntegrationType
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface IntegrationRepository : CrudRepository<IntegrationEntity, UUID> {

    @Query("SELECT * FROM integration ORDER BY name")
    fun findAllOrdered(): List<IntegrationEntity>

    fun findByType(type: IntegrationType): List<IntegrationEntity>

    fun findByIsActive(isActive: Boolean): List<IntegrationEntity>

    fun findByTypeAndIsActive(type: IntegrationType, isActive: Boolean): List<IntegrationEntity>
}
