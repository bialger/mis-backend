package com.bialger.domain.system.repository

import com.bialger.domain.system.entity.AuditLogEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.Instant
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AuditLogRepository : CrudRepository<AuditLogEntity, UUID> {

    @Query(
        value = "SELECT * FROM audit_log ORDER BY timestamp DESC",
        countQuery = "SELECT COUNT(*) FROM audit_log"
    )
    fun findRecent(pageable: Pageable): Page<AuditLogEntity>

    fun findByEmployeeId(employeeId: UUID, pageable: Pageable): Page<AuditLogEntity>

    fun findByEntityType(entityType: String, pageable: Pageable): Page<AuditLogEntity>

    fun findByEmployeeIdAndTimestampBetween(
        employeeId: UUID,
        from: Instant,
        to: Instant
    ): List<AuditLogEntity>

    @Query(
        "SELECT * FROM audit_log WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY timestamp ASC"
    )
    fun findByEntityTypeAndEntityId(entityType: String, entityId: UUID): List<AuditLogEntity>
}
