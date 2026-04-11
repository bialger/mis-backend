package com.bialger.domain.system

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.system.repository.AuditLogRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class AuditLogServiceTest(
    private val auditLogService: AuditLogService,
    private val auditLogRepository: AuditLogRepository,
    private val employeeRepository: EmployeeRepository
) : StringSpec({

    "log() saves an entry with all fields" {
        val actorId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(id = actorId, fullName = "Auditor", email = "audsvc@test.mis", passwordHash = "x", isActive = true)
        )
        val entityId = UUID.randomUUID()

        auditLogService.log(
            actorId = actorId,
            action = "CREATE",
            entityType = AuditLogService.MEDICAL_RECORD,
            entityId = entityId,
            oldValue = null,
            newValue = """{"field":"value"}"""
        )

        val recent = auditLogRepository.findByEmployeeId(actorId, io.micronaut.data.model.Pageable.from(0, 10))
        recent.content.size shouldBe 1
        val saved = recent.content.first()
        saved.action shouldBe "CREATE"
        saved.entityType shouldBe AuditLogService.MEDICAL_RECORD
        saved.entityId shouldBe entityId
        // JSONB normalizes JSON so exact string comparison would be fragile; verify key fields are present
        saved.newValue.shouldNotBeNull()
        saved.newValue!!.contains("field") shouldBe true
        saved.newValue!!.contains("value") shouldBe true
    }

    "log() with no entityId is accepted" {
        val actorId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(id = actorId, fullName = "SysUser", email = "syssvc@test.mis", passwordHash = "x", isActive = true)
        )

        auditLogService.log(actorId = actorId, action = "READ_LIST", entityType = AuditLogService.APPOINTMENT)

        val recent = auditLogRepository.findByEmployeeId(actorId, io.micronaut.data.model.Pageable.from(0, 10))
        recent.content.size shouldBe 1
        recent.content.first().entityId shouldBe null
    }

    "log() does not throw when repository fails (actor does not exist)" {
        val nonExistentActorId = UUID.randomUUID()
        auditLogService.log(
            actorId = nonExistentActorId,
            action = "DELETE",
            entityType = "patient",
            entityId = UUID.randomUUID()
        )
        // no exception should propagate
    }
})
