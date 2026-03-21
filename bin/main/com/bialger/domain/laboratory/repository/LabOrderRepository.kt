package com.bialger.domain.laboratory.repository

import com.bialger.domain.laboratory.entity.LabOrderEntity
import com.bialger.domain.laboratory.enums.LabOrderStatus
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface LabOrderRepository : CrudRepository<LabOrderEntity, UUID> {

    fun findByMedicalRecordId(medicalRecordId: UUID): List<LabOrderEntity>

    fun findByPatientId(patientId: UUID): List<LabOrderEntity>

    fun findByStatus(status: LabOrderStatus): List<LabOrderEntity>
}
