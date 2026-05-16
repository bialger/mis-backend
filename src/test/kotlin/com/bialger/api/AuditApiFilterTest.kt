package com.bialger.api

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.repository.AuditLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Instant
import java.util.UUID

/**
 * Verifies audit log access control: the endpoint requires SYSADMIN role.
 * The TestAutoAuthClientFilter assigns ADMIN role to the integration test user.
 * ADMIN must be rejected with 403 — demonstrating requirement 7 (SYSADMIN-only access).
 */
@MicronautTest
class AuditApiFilterTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val auditLogRepository: AuditLogRepository,
    private val employeeRepository: EmployeeRepository
) : StringSpec({

    val createdEmployeeIds = mutableListOf<UUID>()
    val createdAuditLogIds = mutableListOf<UUID>()

    afterTest {
        createdAuditLogIds.forEach { id -> runCatching { auditLogRepository.deleteById(id) } }
        createdAuditLogIds.clear()
        createdEmployeeIds.forEach { id -> runCatching { employeeRepository.deleteById(id) } }
        createdEmployeeIds.clear()
    }

    "GET /api/audit-logs returns 403 for non-SYSADMIN authenticated user" {
        val ex = runCatching { client.toBlocking().retrieve("/api/audit-logs?page=0&size=10") }
        ex.isFailure shouldBe true
        val httpEx = ex.exceptionOrNull() as? HttpClientResponseException
        httpEx?.status shouldBe HttpStatus.FORBIDDEN
    }

    "GET /api/audit-logs/medical-records returns 403 for non-SYSADMIN authenticated user" {
        val ex = runCatching { client.toBlocking().retrieve("/api/audit-logs/medical-records?page=0&size=10") }
        ex.isFailure shouldBe true
        val httpEx = ex.exceptionOrNull() as? HttpClientResponseException
        httpEx?.status shouldBe HttpStatus.FORBIDDEN
    }

    "GET /api/audit-logs?entityType=MEDICAL_RECORD also returns 403 for non-SYSADMIN" {
        val actorId = UUID.randomUUID()
        createdEmployeeIds += actorId
        employeeRepository.save(
            EmployeeEntity(id = actorId, fullName = "AuditFilterEmp", email = "auditf-${actorId}@test.mis", passwordHash = "x", isActive = true)
        )
        val auditLogId = UUID.randomUUID()
        createdAuditLogIds += auditLogId
        auditLogRepository.save(
            AuditLogEntity(id = auditLogId, employeeId = actorId, action = "READ",
                entityType = "MEDICAL_RECORD", timestamp = Instant.now())
        )

        val ex = runCatching {
            client.toBlocking().retrieve("/api/audit-logs?entityType=MEDICAL_RECORD&page=0&size=100")
        }
        ex.isFailure shouldBe true
        val httpEx = ex.exceptionOrNull() as? HttpClientResponseException
        httpEx?.status shouldBe HttpStatus.FORBIDDEN
    }

    "GET /api/audit-logs/{id} returns 403 for non-SYSADMIN (access check precedes 404)" {
        val ex = runCatching {
            client.toBlocking().retrieve("/api/audit-logs/${UUID.randomUUID()}")
        }
        ex.isFailure shouldBe true
        val httpEx = ex.exceptionOrNull() as? HttpClientResponseException
        httpEx?.status shouldBe HttpStatus.FORBIDDEN
    }
})
