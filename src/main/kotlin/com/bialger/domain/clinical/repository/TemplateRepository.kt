package com.bialger.domain.clinical.repository

import com.bialger.domain.clinical.entity.TemplateEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface TemplateRepository : CrudRepository<TemplateEntity, UUID> {

    @Query("SELECT * FROM template ORDER BY name")
    fun findAllOrdered(): List<TemplateEntity>

    fun findBySpecialtyId(specialtyId: UUID): List<TemplateEntity>

    fun findByEmployeeId(employeeId: UUID): List<TemplateEntity>

    fun findByIsActive(isActive: Boolean): List<TemplateEntity>
}
