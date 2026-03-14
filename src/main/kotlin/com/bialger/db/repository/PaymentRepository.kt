package com.bialger.db.repository

import com.bialger.db.entity.PaymentEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PaymentRepository : CrudRepository<PaymentEntity, UUID> {

    fun findByAppointmentId(appointmentId: UUID): List<PaymentEntity>

    fun findByCreatedBy(createdBy: UUID): List<PaymentEntity>
}
