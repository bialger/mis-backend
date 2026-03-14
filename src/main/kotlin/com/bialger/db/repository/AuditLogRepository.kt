package com.bialger.db.repository

import com.bialger.db.entity.AuditLogEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.Instant
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AuditLogRepository : CrudRepository<AuditLogEntity, UUID> {

    fun findByEmployeeId(employeeId: UUID, pageable: Pageable): Page<AuditLogEntity>

    fun findByEntityType(entityType: String, pageable: Pageable): Page<AuditLogEntity>

    fun findByEmployeeIdAndTimestampBetween(
        employeeId: UUID,
        from: Instant,
        to: Instant
    ): List<AuditLogEntity>
}
