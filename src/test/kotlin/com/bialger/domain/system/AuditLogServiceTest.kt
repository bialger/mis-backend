package com.bialger.domain.system

import com.bialger.domain.system.repository.AuditLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest
class AuditLogServiceTest(
    private val auditLogService: AuditLogService,
    private val auditLogRepository: AuditLogRepository,
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper
) : StringSpec({

    val createdEmployeeIds = mutableListOf<UUID>()
    val createdAuditActorIds = mutableListOf<UUID>()

    /**
     * Creates an employee via HTTP (committed immediately, visible to REQUIRES_NEW audit transactions).
     */
    fun createEmployee(suffix: String): UUID {
        val roleResp = client.toBlocking().retrieve("/api/catalog/roles")
        val roleId = objectMapper.readTree(roleResp)[0].path("id").asText()
        val body = """{
            "fullName": "AuditTester-$suffix",
            "email": "audit-$suffix@test.mis",
            "password": "Test1234!",
            "isActive": true,
            "roleId": "$roleId"
        }"""
        val resp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/employees", body).contentType(MediaType.APPLICATION_JSON)
        )
        val id = UUID.fromString(objectMapper.readTree(resp).path("id").asText())
        createdEmployeeIds += id
        return id
    }

    afterTest {
        createdAuditActorIds.forEach { actorId ->
            runCatching { auditLogRepository.findByEmployeeId(actorId, io.micronaut.data.model.Pageable.from(0, 100)).content.forEach { auditLogRepository.deleteById(it.id) } }
        }
        createdAuditActorIds.clear()
        createdEmployeeIds.forEach { id ->
            runCatching { client.toBlocking().exchange<Any, Any>(HttpRequest.DELETE("/api/employees/$id")) }
        }
        createdEmployeeIds.clear()
    }

    "log() saves an entry with all fields" {
        val actorId = createEmployee(UUID.randomUUID().toString().take(8))
        createdAuditActorIds += actorId
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
        saved.newValue.shouldNotBeNull()
        saved.newValue!!.contains("field") shouldBe true
        saved.newValue!!.contains("value") shouldBe true
    }

    "log() with no entityId is accepted" {
        val actorId = createEmployee(UUID.randomUUID().toString().take(8))
        createdAuditActorIds += actorId

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
