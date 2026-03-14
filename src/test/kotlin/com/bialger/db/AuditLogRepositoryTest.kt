package com.bialger.db

import com.bialger.db.entity.AuditLogEntity
import com.bialger.db.entity.EmployeeEntity
import com.bialger.db.repository.AuditLogRepository
import com.bialger.db.repository.EmployeeRepository
import io.micronaut.data.model.Pageable
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.time.Instant
import java.util.UUID

@MicronautTest(transactional = true)
class AuditLogRepositoryTest(
    private val auditLogRepository: AuditLogRepository,
    private val employeeRepository: EmployeeRepository
) : StringSpec({

    "save and findById" {
        val employeeId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(id = employeeId, fullName = "Auditor", email = "audit@test.mis", passwordHash = "x", isActive = true)
        )

        val entity = AuditLogEntity(
            id = UUID.randomUUID(),
            employeeId = employeeId,
            action = "CREATE",
            entityType = "patient",
            entityId = UUID.randomUUID(),
            newValue = """{"name":"Иван"}""",
            timestamp = Instant.now()
        )
        auditLogRepository.save(entity)

        val found = auditLogRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.action shouldBe "CREATE"
        found.entityType shouldBe "patient"
    }

    "findByEmployeeId" {
        val employeeId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(id = employeeId, fullName = "User", email = "user@test.mis", passwordHash = "x", isActive = true)
        )

        auditLogRepository.save(
            AuditLogEntity(
                id = UUID.randomUUID(),
                employeeId = employeeId,
                action = "UPDATE",
                entityType = "appointment",
                timestamp = Instant.now()
            )
        )
        auditLogRepository.save(
            AuditLogEntity(
                id = UUID.randomUUID(),
                employeeId = employeeId,
                action = "DELETE",
                entityType = "patient",
                timestamp = Instant.now()
            )
        )

        val page = auditLogRepository.findByEmployeeId(employeeId, Pageable.from(0, 100))
        page.content shouldHaveSize 2
        page.content.map { it.entityType }.toSet() shouldBe setOf("appointment", "patient")
    }

    "findByEntityType" {
        val employeeId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(id = employeeId, fullName = "Emp", email = "emp@test.mis", passwordHash = "x", isActive = true)
        )

        auditLogRepository.save(
            AuditLogEntity(
                id = UUID.randomUUID(),
                employeeId = employeeId,
                action = "CREATE",
                entityType = "payment",
                timestamp = Instant.now()
            )
        )
        auditLogRepository.save(
            AuditLogEntity(
                id = UUID.randomUUID(),
                employeeId = employeeId,
                action = "UPDATE",
                entityType = "payment",
                timestamp = Instant.now()
            )
        )

        val paymentPage = auditLogRepository.findByEntityType("payment", Pageable.from(0, 100))
        paymentPage.content shouldHaveSize 2
    }
})
