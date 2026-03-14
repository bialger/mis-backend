package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("audit_log")
data class AuditLogEntity(
    @Id val id: UUID,
    val employeeId: UUID,
    val action: String,
    val entityType: String,
    val entityId: UUID? = null,
    val oldValue: String? = null,   // JSONB as String
    val newValue: String? = null,   // JSONB as String
    val ipAddress: String? = null,
    val timestamp: Instant
)
