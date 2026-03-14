package com.bialger.db.repository

import com.bialger.db.entity.AppointmentServiceEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AppointmentServiceRepository : CrudRepository<AppointmentServiceEntity, UUID> {

    fun findByAppointmentId(appointmentId: UUID): List<AppointmentServiceEntity>

    fun findByServiceId(serviceId: UUID): List<AppointmentServiceEntity>
}
