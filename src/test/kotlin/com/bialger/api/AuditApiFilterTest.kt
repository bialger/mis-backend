package com.bialger.api

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.repository.AuditLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Instant
import java.util.UUID

@MicronautTest
class AuditApiFilterTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val auditLogRepository: AuditLogRepository,
    private val employeeRepository: EmployeeRepository
) : StringSpec({

    "GET /api/audit-logs returns paged content" {
        val body = client.toBlocking().retrieve("/api/audit-logs?page=0&size=10")
        val tree = objectMapper.readTree(body)
        tree.path("content").isArray shouldBe true
    }

    "GET /api/audit-logs?entityType=MEDICAL_RECORD filters by type" {
        val actorId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(id = actorId, fullName = "AuditFilterEmp", email = "auditf-${actorId}@test.mis", passwordHash = "x", isActive = true)
        )
        auditLogRepository.save(
            AuditLogEntity(id = UUID.randomUUID(), employeeId = actorId, action = "READ",
                entityType = "MEDICAL_RECORD", timestamp = Instant.now())
        )

        val body = client.toBlocking().retrieve(
            "/api/audit-logs?entityType=MEDICAL_RECORD&page=0&size=100"
        )
        val tree = objectMapper.readTree(body)
        val content = tree.path("content")
        content.isArray shouldBe true
        content.all { it.path("entityType").asText() == "MEDICAL_RECORD" } shouldBe true
    }

    "GET /api/audit-logs/medical-records returns medical record audit entries" {
        val body = client.toBlocking().retrieve("/api/audit-logs/medical-records?page=0&size=10")
        val tree = objectMapper.readTree(body)
        tree.path("content").isArray shouldBe true
        tree.path("content").all { it.path("entityType").asText() == "MEDICAL_RECORD" } shouldBe true
    }

    "GET /api/audit-logs/{id} returns 404 for unknown id" {
        val ex = kotlin.runCatching {
            client.toBlocking().retrieve("/api/audit-logs/${UUID.randomUUID()}")
        }
        ex.isFailure shouldBe true
    }
})
