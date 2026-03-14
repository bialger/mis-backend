package com.bialger.db.repository

import com.bialger.db.entity.PatientTagTypeEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PatientTagTypeRepository : CrudRepository<PatientTagTypeEntity, UUID> {

    fun findByCode(code: String): PatientTagTypeEntity?

    fun findByIsActive(isActive: Boolean): List<PatientTagTypeEntity>
}
