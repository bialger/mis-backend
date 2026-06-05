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

    // --- Filtered paged queries for API ---

    @Query(
        value = """
            SELECT * FROM audit_log
            WHERE entity_type = :entityType
              AND timestamp >= :from
              AND timestamp <= :to
            ORDER BY timestamp DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM audit_log
            WHERE entity_type = :entityType
              AND timestamp >= :from
              AND timestamp <= :to
        """
    )
    fun findByEntityTypeAndDateRange(
        entityType: String,
        from: Instant,
        to: Instant,
        pageable: Pageable
    ): Page<AuditLogEntity>

    @Query(
        value = """
            SELECT * FROM audit_log
            WHERE entity_type = :entityType
              AND entity_id = :entityId
              AND timestamp >= :from
              AND timestamp <= :to
            ORDER BY timestamp DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM audit_log
            WHERE entity_type = :entityType
              AND entity_id = :entityId
              AND timestamp >= :from
              AND timestamp <= :to
        """
    )
    fun findByEntityTypeAndEntityIdAndDateRange(
        entityType: String,
        entityId: UUID,
        from: Instant,
        to: Instant,
        pageable: Pageable
    ): Page<AuditLogEntity>

    @Query(
        value = """
            SELECT * FROM audit_log
            WHERE timestamp >= :from
              AND timestamp <= :to
            ORDER BY timestamp DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM audit_log
            WHERE timestamp >= :from
              AND timestamp <= :to
        """
    )
    fun findByDateRange(from: Instant, to: Instant, pageable: Pageable): Page<AuditLogEntity>

    // --- Action-filtered paged queries ---

    @Query(
        value = """
            SELECT * FROM audit_log
            WHERE action = :action
              AND timestamp >= :from
              AND timestamp <= :to
            ORDER BY timestamp DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM audit_log
            WHERE action = :action
              AND timestamp >= :from
              AND timestamp <= :to
        """
    )
    fun findByActionAndDateRange(
        action: String,
        from: Instant,
        to: Instant,
        pageable: Pageable
    ): Page<AuditLogEntity>

    @Query(
        value = """
            SELECT * FROM audit_log
            WHERE entity_type = :entityType
              AND action = :action
              AND timestamp >= :from
              AND timestamp <= :to
            ORDER BY timestamp DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM audit_log
            WHERE entity_type = :entityType
              AND action = :action
              AND timestamp >= :from
              AND timestamp <= :to
        """
    )
    fun findByEntityTypeAndActionAndDateRange(
        entityType: String,
        action: String,
        from: Instant,
        to: Instant,
        pageable: Pageable
    ): Page<AuditLogEntity>

    // --- Retention cleanup ---

    @Query("DELETE FROM audit_log WHERE timestamp < :cutoff")
    fun deleteOlderThan(cutoff: Instant): Long
}
