package com.bialger.db.repository

import com.bialger.db.entity.PrescriptionEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PrescriptionRepository : CrudRepository<PrescriptionEntity, UUID> {

    fun findByMedicalRecordId(medicalRecordId: UUID): List<PrescriptionEntity>
}
