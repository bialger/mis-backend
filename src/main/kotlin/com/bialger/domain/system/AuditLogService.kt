package com.bialger.domain.system

import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.repository.AuditLogRepository
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Central service for writing audit log entries.
 * Failures are swallowed to avoid disrupting the main operation flow.
 */
@Singleton
class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun log(
        actorId: UUID,
        action: String,
        entityType: String,
        entityId: UUID? = null,
        oldValue: String? = null,
        newValue: String? = null
    ) {
        runCatching {
            auditLogRepository.save(
                AuditLogEntity(
                    id = UUID.randomUUID(),
                    employeeId = actorId,
                    action = action,
                    entityType = entityType,
                    entityId = entityId,
                    oldValue = oldValue,
                    newValue = newValue,
                    timestamp = Instant.now()
                )
            )
        }.onFailure { e ->
            log.warn("Failed to write audit log [action={}, entity={}/{}]: {}", action, entityType, entityId, e.message)
        }
    }

    companion object {
        const val MEDICAL_RECORD = "MEDICAL_RECORD"
        const val APPOINTMENT = "APPOINTMENT"
        const val PATIENT_CONSENT = "PATIENT_CONSENT"
        const val INVENTORY_OPERATION = "INVENTORY_OPERATION"
    }
}
