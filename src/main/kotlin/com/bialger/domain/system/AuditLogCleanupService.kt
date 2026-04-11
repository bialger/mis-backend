package com.bialger.domain.system

import com.bialger.domain.system.repository.AuditLogRepository
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Scheduled cleanup: removes audit log entries older than 90 days (3 months).
 * Runs daily at 03:00 to avoid peak load. Also runs 10 minutes after startup.
 */
@Singleton
open class AuditLogCleanupService(
    private val auditLogRepository: AuditLogRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *", initialDelay = "10m")
    open fun cleanupOldLogs() {
        val cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS)
        runCatching {
            val deleted = auditLogRepository.deleteOlderThan(cutoff)
            if (deleted > 0) {
                log.info("Audit log cleanup: removed {} entries older than {} days.", deleted, RETENTION_DAYS)
            }
        }.onFailure { e ->
            log.error("Audit log cleanup failed: {}", e.message)
        }
    }

    companion object {
        const val RETENTION_DAYS = 90L
    }
}
