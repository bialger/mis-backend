package com.bialger.db

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.repository.AuditLogRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.micronaut.data.model.Pageable
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@MicronautTest(transactional = true)
class AuditLogRepositoryFilterTest(
    private val auditLogRepository: AuditLogRepository,
    private val employeeRepository: EmployeeRepository
) : StringSpec({

    fun employee(email: String): UUID {
        val id = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = id, fullName = "Filter Tester", email = email, passwordHash = "x", isActive = true))
        return id
    }

    fun log(actorId: UUID, entityType: String, entityId: UUID? = null, ts: Instant = Instant.now()): AuditLogEntity {
        val e = AuditLogEntity(
            id = UUID.randomUUID(),
            employeeId = actorId,
            action = "CREATE",
            entityType = entityType,
            entityId = entityId,
            timestamp = ts
        )
        auditLogRepository.save(e)
        return e
    }

    "findByDateRange returns entries within the range" {
        val actorId = employee("dr@filter.mis")
        val from = Instant.now().minus(5, ChronoUnit.DAYS)
        val to = Instant.now().plus(1, ChronoUnit.DAYS)

        log(actorId, "patient", ts = Instant.now().minus(2, ChronoUnit.DAYS))
        log(actorId, "patient", ts = Instant.now().minus(10, ChronoUnit.DAYS))

        val page = auditLogRepository.findByDateRange(from, to, Pageable.from(0, 100))
        page.content.any { it.employeeId == actorId && it.timestamp.isAfter(from) } shouldBe true
    }

    "findByEntityTypeAndDateRange returns only matching entityType" {
        val actorId = employee("typ@filter.mis")
        val from = Instant.now().minus(1, ChronoUnit.DAYS)
        val to = Instant.now().plus(1, ChronoUnit.DAYS)

        log(actorId, "MEDICAL_RECORD")
        log(actorId, "APPOINTMENT")
        log(actorId, "MEDICAL_RECORD")

        val mrPage = auditLogRepository.findByEntityTypeAndDateRange("MEDICAL_RECORD", from, to, Pageable.from(0, 100))
        mrPage.content.all { it.entityType == "MEDICAL_RECORD" } shouldBe true
        mrPage.content.count { it.employeeId == actorId } shouldBe 2
    }

    "findByEntityTypeAndEntityIdAndDateRange narrows by both type and entityId" {
        val actorId = employee("eid@filter.mis")
        val entityId = UUID.randomUUID()
        val from = Instant.now().minus(1, ChronoUnit.DAYS)
        val to = Instant.now().plus(1, ChronoUnit.DAYS)

        log(actorId, "MEDICAL_RECORD", entityId)
        log(actorId, "MEDICAL_RECORD", UUID.randomUUID())

        val page = auditLogRepository.findByEntityTypeAndEntityIdAndDateRange(
            "MEDICAL_RECORD", entityId, from, to, Pageable.from(0, 100)
        )
        page.content shouldHaveSize 1
        page.content.first().entityId shouldBe entityId
    }

    "deleteOlderThan removes only entries before cutoff" {
        val actorId = employee("del@filter.mis")
        val cutoff = Instant.now().minus(30, ChronoUnit.DAYS)

        val old = log(actorId, "patient", ts = Instant.now().minus(60, ChronoUnit.DAYS))
        val recent = log(actorId, "patient", ts = Instant.now())

        auditLogRepository.deleteOlderThan(cutoff)

        auditLogRepository.findById(old.id).isPresent shouldBe false
        auditLogRepository.findById(recent.id).isPresent shouldBe true
    }
})
