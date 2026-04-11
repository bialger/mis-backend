package com.bialger.domain.system

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.repository.AuditLogRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@MicronautTest(transactional = true)
class AuditLogCleanupServiceTest(
    private val auditLogCleanupService: AuditLogCleanupService,
    private val auditLogRepository: AuditLogRepository,
    private val employeeRepository: EmployeeRepository
) : StringSpec({

    "cleanupOldLogs removes entries older than 90 days" {
        val actorId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(id = actorId, fullName = "Cleanup Tester", email = "cleanup@test.mis", passwordHash = "x", isActive = true)
        )

        val oldTimestamp = Instant.now().minus(100, ChronoUnit.DAYS)
        val recentTimestamp = Instant.now().minus(10, ChronoUnit.DAYS)

        val oldEntry = AuditLogEntity(
            id = UUID.randomUUID(),
            employeeId = actorId,
            action = "CREATE",
            entityType = "patient",
            timestamp = oldTimestamp
        )
        val recentEntry = AuditLogEntity(
            id = UUID.randomUUID(),
            employeeId = actorId,
            action = "UPDATE",
            entityType = "patient",
            timestamp = recentTimestamp
        )
        auditLogRepository.save(oldEntry)
        auditLogRepository.save(recentEntry)

        auditLogCleanupService.cleanupOldLogs()

        auditLogRepository.findById(oldEntry.id).isPresent shouldBe false
        auditLogRepository.findById(recentEntry.id).isPresent shouldBe true
    }

    "cleanupOldLogs leaves recent entries untouched" {
        val actorId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(id = actorId, fullName = "Recent Actor", email = "recent@test.mis", passwordHash = "x", isActive = true)
        )

        val recentEntry = AuditLogEntity(
            id = UUID.randomUUID(),
            employeeId = actorId,
            action = "READ",
            entityType = "medical_record",
            timestamp = Instant.now()
        )
        auditLogRepository.save(recentEntry)

        auditLogCleanupService.cleanupOldLogs()

        auditLogRepository.findById(recentEntry.id).isPresent shouldBe true
    }
})
