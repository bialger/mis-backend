package com.bialger.domain.patient.repository

import com.bialger.domain.patient.entity.PatientEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PatientRepository : CrudRepository<PatientEntity, UUID> {

    @Query("SELECT * FROM patient ORDER BY full_name")
    fun findAllOrdered(): List<PatientEntity>

    fun findByCardNumber(cardNumber: String): PatientEntity?

    fun findByOrganizationId(organizationId: UUID): List<PatientEntity>

    fun existsByCardNumber(cardNumber: String): Boolean
}
