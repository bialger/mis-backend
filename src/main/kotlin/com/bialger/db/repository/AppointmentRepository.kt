package com.bialger.db.repository

import com.bialger.db.entity.AppointmentEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AppointmentRepository : CrudRepository<AppointmentEntity, UUID> {

    fun findByPatientId(patientId: UUID): List<AppointmentEntity>

    fun findByEmployeeId(employeeId: UUID): List<AppointmentEntity>

    fun findByBranchId(branchId: UUID): List<AppointmentEntity>
}
