package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Introspected
@Schema(description = "Diff object inside an audit log entry")
data class AuditDiffDto(
    val old: String?,
    val new: String?
)

@Serdeable
@Introspected
@Schema(description = "Audit log entry response")
data class AuditLogEntryDto(
    val id: String,
    val employeeId: String,
    /** Alias for employeeId — kept for SPA backward compatibility. */
    val userId: String,
    val employeeName: String,
    val action: String,
    val entityType: String,
    val entityId: String?,
    val oldValue: String?,
    val newValue: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val timestamp: String,
    /** Alias for timestamp — kept for SPA backward compatibility. */
    val ts: String,
    /** Nested diff object for SPA rendering. */
    val diff: AuditDiffDto
)

@Serdeable
@Introspected
@Schema(description = "Patient consent response")
data class PatientConsentRestDto(
    val id: String,
    val patientId: String,
    val consentType: String,
    val isGranted: Boolean,
    val grantedAt: String?,
    val revokedAt: String?
)

@Serdeable
@Introspected
@Schema(description = "Inventory operation response")
data class InventoryOperationRestDto(
    val id: String,
    val itemId: String,
    val itemName: String?,
    val employeeId: String,
    val employeeName: String?,
    val operationType: String,
    val quantity: String,
    val appointmentId: String?,
    val notes: String?,
    val createdAt: String?
)
