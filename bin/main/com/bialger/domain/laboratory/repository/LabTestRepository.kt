package com.bialger.domain.laboratory.repository

import com.bialger.domain.laboratory.entity.LabTestEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface LabTestRepository : CrudRepository<LabTestEntity, UUID> {

    @Query("SELECT * FROM lab_test ORDER BY name")
    fun findAllOrdered(): List<LabTestEntity>

    fun findByLaboratoryId(laboratoryId: UUID): List<LabTestEntity>

    fun findByIsActive(isActive: Boolean): List<LabTestEntity>
}
