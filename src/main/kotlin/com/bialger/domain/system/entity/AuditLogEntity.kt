package com.bialger.domain.system.entity

import com.bialger.db.converter.JsonbConverter
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("audit_log")
data class AuditLogEntity(
    @Id val id: UUID,
    val employeeId: UUID,
    val action: String,
    val entityType: String,
    val entityId: UUID? = null,
    @field:TypeDef(type = DataType.OBJECT, converter = JsonbConverter::class)
    val oldValue: String? = null,
    @field:TypeDef(type = DataType.OBJECT, converter = JsonbConverter::class)
    val newValue: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val timestamp: Instant
)
