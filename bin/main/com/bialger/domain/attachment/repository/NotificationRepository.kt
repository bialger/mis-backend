package com.bialger.domain.attachment.repository

import com.bialger.domain.attachment.entity.NotificationEntity
import com.bialger.domain.attachment.enums.NotificationStatus
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface NotificationRepository : CrudRepository<NotificationEntity, UUID> {

    fun findByPatientId(patientId: UUID): List<NotificationEntity>

    fun findByStatus(status: NotificationStatus): List<NotificationEntity>

    fun findByPatientIdAndStatus(patientId: UUID, status: NotificationStatus): List<NotificationEntity>
}
