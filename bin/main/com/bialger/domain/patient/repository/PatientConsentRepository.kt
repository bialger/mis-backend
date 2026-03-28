package com.bialger.domain.patient.repository

import com.bialger.domain.patient.entity.PatientConsentEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PatientConsentRepository : CrudRepository<PatientConsentEntity, UUID> {

    fun findByPatientId(patientId: UUID): List<PatientConsentEntity>
}
