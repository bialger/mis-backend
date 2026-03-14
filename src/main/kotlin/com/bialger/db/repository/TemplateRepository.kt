package com.bialger.db.repository

import com.bialger.db.entity.TemplateEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface TemplateRepository : CrudRepository<TemplateEntity, UUID> {

    fun findBySpecialtyId(specialtyId: UUID): List<TemplateEntity>

    fun findByEmployeeId(employeeId: UUID): List<TemplateEntity>

    fun findByIsActive(isActive: Boolean): List<TemplateEntity>
}
