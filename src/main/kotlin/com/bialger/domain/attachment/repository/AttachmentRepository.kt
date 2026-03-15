package com.bialger.domain.attachment.repository

import com.bialger.domain.attachment.entity.AttachmentEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AttachmentRepository : CrudRepository<AttachmentEntity, UUID> {

    fun findByPatientId(patientId: UUID): List<AttachmentEntity>

    fun findByAppointmentId(appointmentId: UUID): List<AttachmentEntity>

    fun findByMedicalRecordId(medicalRecordId: UUID): List<AttachmentEntity>
}
