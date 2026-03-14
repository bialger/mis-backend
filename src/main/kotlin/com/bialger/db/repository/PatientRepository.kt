package com.bialger.db.repository

import com.bialger.db.entity.PatientEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PatientRepository : CrudRepository<PatientEntity, UUID> {

    fun findByCardNumber(cardNumber: String): PatientEntity?

    fun findByOrganizationId(organizationId: UUID): List<PatientEntity>

    fun existsByCardNumber(cardNumber: String): Boolean
}
