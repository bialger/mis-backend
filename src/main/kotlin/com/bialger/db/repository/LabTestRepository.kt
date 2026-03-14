package com.bialger.db.repository

import com.bialger.db.entity.LabTestEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface LabTestRepository : CrudRepository<LabTestEntity, UUID> {

    fun findByLaboratoryId(laboratoryId: UUID): List<LabTestEntity>

    fun findByIsActive(isActive: Boolean): List<LabTestEntity>
}
