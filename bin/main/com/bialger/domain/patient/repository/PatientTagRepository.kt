package com.bialger.domain.patient.repository

import com.bialger.domain.patient.entity.PatientTagEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PatientTagRepository : CrudRepository<PatientTagEntity, UUID> {

    fun findByPatientId(patientId: UUID): List<PatientTagEntity>

    fun findByTagTypeId(tagTypeId: UUID): List<PatientTagEntity>
}
