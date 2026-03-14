package com.bialger.db.repository

import com.bialger.db.entity.InsertSheetEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface InsertSheetRepository : CrudRepository<InsertSheetEntity, UUID> {

    fun findByPatientId(patientId: UUID): List<InsertSheetEntity>

    fun findByAppointmentId(appointmentId: UUID): List<InsertSheetEntity>
}
