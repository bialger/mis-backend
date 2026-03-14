package com.bialger.db.repository

import com.bialger.db.entity.DiagnosisEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface DiagnosisRepository : CrudRepository<DiagnosisEntity, UUID> {

    fun findByMedicalRecordId(medicalRecordId: UUID): List<DiagnosisEntity>

    fun findByIsPrimary(isPrimary: Boolean): List<DiagnosisEntity>
}
