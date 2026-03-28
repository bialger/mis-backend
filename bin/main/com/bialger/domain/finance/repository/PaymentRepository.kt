package com.bialger.domain.finance.repository

import com.bialger.domain.finance.entity.PaymentEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PaymentRepository : CrudRepository<PaymentEntity, UUID> {

    @Query("SELECT * FROM payment ORDER BY created_at DESC NULLS LAST, id")
    fun findAllOrdered(): List<PaymentEntity>

    fun findByAppointmentId(appointmentId: UUID): List<PaymentEntity>

    fun findByCreatedBy(createdBy: UUID): List<PaymentEntity>
}
