package com.bialger.domain.clinical.repository

import com.bialger.domain.clinical.entity.MedicalRecordEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface MedicalRecordRepository : CrudRepository<MedicalRecordEntity, UUID> {

    fun findByAppointmentId(appointmentId: UUID): MedicalRecordEntity?

    fun findByPatientId(patientId: UUID): List<MedicalRecordEntity>

    fun findByEmployeeId(employeeId: UUID): List<MedicalRecordEntity>
}
