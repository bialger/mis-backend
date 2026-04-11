package com.bialger.domain.system

import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.repository.AuditLogRepository
import io.micronaut.transaction.annotation.Transactional
import io.micronaut.transaction.TransactionDefinition
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Central service for writing audit log entries.
 * Failures are swallowed to avoid disrupting the main operation flow.
 *
 * REQUIRES_NEW ensures the INSERT runs in its own read-write transaction even
 * when called from a @Transactional(readOnly = true) context. Without this,
 * PostgreSQL aborts the outer read-only transaction on the INSERT attempt,
 * causing all subsequent queries in that transaction to fail with
 * "current transaction is aborted".
 */
@Singleton
open class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    open fun log(
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
