package com.bialger.db.repository

import com.bialger.db.entity.LabOrderEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface LabOrderRepository : CrudRepository<LabOrderEntity, UUID> {

    fun findByMedicalRecordId(medicalRecordId: UUID): List<LabOrderEntity>

    fun findByPatientId(patientId: UUID): List<LabOrderEntity>

    fun findByStatus(status: String): List<LabOrderEntity>
}
