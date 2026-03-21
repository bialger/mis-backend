package com.bialger.domain.patient.repository

import com.bialger.domain.patient.entity.PatientTagTypeEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PatientTagTypeRepository : CrudRepository<PatientTagTypeEntity, UUID> {

    @Query("SELECT * FROM patient_tag_type ORDER BY name")
    fun findAllOrdered(): List<PatientTagTypeEntity>

    fun findByCode(code: String): PatientTagTypeEntity?

    fun findByIsActive(isActive: Boolean): List<PatientTagTypeEntity>
}
