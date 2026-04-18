package com.bialger.api

import com.bialger.auth.domain.PasswordHasher
import com.bialger.domain.attachment.repository.AttachmentRepository
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.infrastructure.StorageServiceStub
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.context.annotation.Property
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.transaction.SynchronousTransactionManager
import java.sql.Connection
import java.time.Instant
import java.util.UUID

@MicronautTest(transactional = false)
@Property(name = "JWT_SECRET", value = "test-jwt-secret-for-integration-tests-only-1234567890")
class AttachmentApiTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val attachmentRepository: AttachmentRepository,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val roleRepository: RoleRepository,
    private val passwordHasher: PasswordHasher,
    private val transactionManager: SynchronousTransactionManager<Connection>,
    private val storageStub: StorageServiceStub
) : StringSpec({

    val createdAttachmentIds = mutableListOf<UUID>()
    val createdPatientIds = mutableListOf<UUID>()
    val createdEmployeeIds = mutableListOf<UUID>()

    /**
     * Returns the organizationId from seeded branches (V15 seeds a default org + branch).
     * Falls back to the well-known seeded org UUID if the API returns nothing.
     */
    fun orgId(): String {
        val resp = client.toBlocking().retrieve("/api/branches?page=0&size=1")
        val content = objectMapper.readTree(resp).path("content")
        return if (content.isArray && content.size() > 0) {
            content[0].path("organizationId").asText()
        } else {
            "a0000001-0000-4000-8000-000000000001"
        }
    }

    /**
     * Creates a patient via HTTP API (ensures the record is committed before further requests).
     */
    fun createPatient(): String {
        val cardNumber = "ATT-${UUID.randomUUID().toString().take(8)}"
        val body = """{"organizationId":"${orgId()}","cardNumber":"$cardNumber","fullName":"AttachTest Patient"}"""
        val resp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/patients", body).contentType(MediaType.APPLICATION_JSON)
        )
        val id = objectMapper.readTree(resp).path("id").asText()
        runCatching { createdPatientIds += UUID.fromString(id) }
        return id
    }

    /**
     * Creates a dedicated employee per call so parallel specs cannot invalidate `uploadedBy` via shared
     * integration-auth cleanup (FK failures surfaced as conflict responses).
     */
    fun createEmployee(): String = transactionManager.executeWrite {
        val id = UUID.randomUUID()
        val email = "attach-${id.toString().take(8)}@mis.local"
        employeeRepository.save(
            EmployeeEntity(
                id = id,
                fullName = "Attachment test actor",
                email = email,
                phone = null,
                passwordHash = passwordHasher.hash("AttachActorPass123!"),
                mustChangePassword = false,
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val role = roleRepository.findByName("ADMIN")
            ?: roleRepository.findByName("SYSADMIN")
            ?: roleRepository.findAllOrdered().firstOrNull()
            ?: error("No role for attachment test employee")
        employeeRoleRepository.save(id, role.id)
        createdEmployeeIds += id
        id.toString()
    }

    afterTest {
        storageStub.reset()
        createdAttachmentIds.forEach { runCatching { attachmentRepository.deleteById(it) } }
        createdAttachmentIds.clear()
        createdPatientIds.forEach { runCatching { patientRepository.deleteById(it) } }
        createdPatientIds.clear()
        transactionManager.executeWrite {
            createdEmployeeIds.forEach { eid ->
                runCatching { employeeRoleRepository.deleteByEmployeeId(eid) }
                runCatching { employeeRepository.deleteById(eid) }
            }
        }
        createdEmployeeIds.clear()
    }

    "POST /api/attachments/upload creates attachment and returns DTO with S3 URL" {
        val patientId = createPatient()
        val empId = createEmployee()

        val body = MultipartBody.builder()
            .addPart("file", "test-report.pdf", MediaType.of("application/pdf"), "PDF content".toByteArray())
            .addPart("patientId", patientId)
            .addPart("uploadedBy", empId)
            .build()

        val response = client.toBlocking().retrieve(
            HttpRequest.POST("/api/attachments/upload", body)
                .contentType(MediaType.MULTIPART_FORM_DATA),
            String::class.java
        )

        val tree = objectMapper.readTree(response)
        tree.path("patientId").asText() shouldBe patientId
        tree.path("fileName").asText() shouldBe "test-report.pdf"
        tree.path("fileType").asText() shouldBe "PDF"
        val filePath = tree.path("filePath").asText()
        filePath shouldContain "storage.yandexcloud.net"
        // Original filename is stored in DB (fileName field), not exposed in the S3 URL

        val attachmentId = UUID.fromString(tree.path("id").asText())
        createdAttachmentIds += attachmentId

        storageStub.uploads shouldHaveSize 1
        storageStub.uploads.first().contentType shouldBe "application/pdf"
    }

    "GET /api/attachments?patientId returns uploaded attachments" {
        val patientId = createPatient()
        val empId = createEmployee()

        val body = MultipartBody.builder()
            .addPart("file", "photo.png", MediaType.of("image/png"), byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
            .addPart("patientId", patientId)
            .addPart("uploadedBy", empId)
            .build()

        val uploadResp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/attachments/upload", body).contentType(MediaType.MULTIPART_FORM_DATA),
            String::class.java
        )
        val attachmentId = UUID.fromString(objectMapper.readTree(uploadResp).path("id").asText())
        createdAttachmentIds += attachmentId

        val listResp = client.toBlocking().retrieve(
            HttpRequest.GET<String>("/api/attachments?patientId=$patientId"),
            String::class.java
        )
        val arr = objectMapper.readTree(listResp)
        arr.isArray shouldBe true
        (arr.size() >= 1) shouldBe true
        arr.any { it.path("id").asText() == attachmentId.toString() } shouldBe true
    }

    "POST /api/attachments/upload with unsupported content type returns 400" {
        val patientId = createPatient()
        val empId = createEmployee()

        val body = MultipartBody.builder()
            .addPart("file", "script.exe", MediaType.of("application/octet-stream"), byteArrayOf(0x4D, 0x5A))
            .addPart("patientId", patientId)
            .addPart("uploadedBy", empId)
            .build()

        val ex = kotlin.runCatching {
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/attachments/upload", body).contentType(MediaType.MULTIPART_FORM_DATA),
                String::class.java
            )
        }
        ex.isFailure shouldBe true
        val status = (ex.exceptionOrNull() as? HttpClientResponseException)?.status
        status shouldBe HttpStatus.BAD_REQUEST

        storageStub.uploads shouldHaveSize 0
    }

    "POST /api/attachments/upload with empty file returns 400" {
        val patientId = createPatient()
        val empId = createEmployee()

        val body = MultipartBody.builder()
            .addPart("file", "empty.pdf", MediaType.of("application/pdf"), ByteArray(0))
            .addPart("patientId", patientId)
            .addPart("uploadedBy", empId)
            .build()

        val ex = kotlin.runCatching {
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/attachments/upload", body).contentType(MediaType.MULTIPART_FORM_DATA),
                String::class.java
            )
        }
        ex.isFailure shouldBe true
        val status = (ex.exceptionOrNull() as? HttpClientResponseException)?.status
        status shouldBe HttpStatus.BAD_REQUEST
    }

    "GET /api/attachments/{id}/download returns file bytes with correct headers" {
        val patientId = createPatient()
        val empId = createEmployee()
        val fileContent = "Hello, PDF!".toByteArray()

        val body = MultipartBody.builder()
            .addPart("file", "report-2025.pdf", MediaType.of("application/pdf"), fileContent)
            .addPart("patientId", patientId)
            .addPart("uploadedBy", empId)
            .build()

        val uploadResp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/attachments/upload", body).contentType(MediaType.MULTIPART_FORM_DATA),
            String::class.java
        )
        val attachmentId = objectMapper.readTree(uploadResp).path("id").asText()
        createdAttachmentIds += UUID.fromString(attachmentId)

        val response = client.toBlocking().exchange(
            HttpRequest.GET<ByteArray>("/api/attachments/$attachmentId/download"),
            ByteArray::class.java
        )
        response.status shouldBe HttpStatus.OK
        val disposition = response.headers.get("Content-Disposition") ?: ""
        disposition shouldContain "attachment"
        disposition shouldContain "report-2025.pdf"
        response.headers.get("Content-Type") shouldContain "application/pdf"
    }

    "GET /api/attachments/{id}/download sets RFC 5987 filename* for non-ASCII filenames" {
        val patientId = createPatient()
        val empId = createEmployee()

        val body = MultipartBody.builder()
            .addPart("file", "отчёт.pdf", MediaType.of("application/pdf"), "content".toByteArray())
            .addPart("patientId", patientId)
            .addPart("uploadedBy", empId)
            .build()

        val uploadResp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/attachments/upload", body).contentType(MediaType.MULTIPART_FORM_DATA),
            String::class.java
        )
        val attachmentId = objectMapper.readTree(uploadResp).path("id").asText()
        createdAttachmentIds += UUID.fromString(attachmentId)

        val response = client.toBlocking().exchange(
            HttpRequest.GET<ByteArray>("/api/attachments/$attachmentId/download"),
            ByteArray::class.java
        )
        response.status shouldBe HttpStatus.OK
        val disposition = response.headers.get("Content-Disposition") ?: ""
        // RFC 5987: filename*=UTF-8'' + percent-encoded name
        disposition shouldContain "filename*=UTF-8''"
        disposition shouldContain "%D0%BE%D1%82%D1%87%D1%91%D1%82"   // Cyrillic word for report in filename, URL-encoded
    }

    "GET /api/attachments/{id}/download for unknown id returns 404" {
        val ex = kotlin.runCatching {
            client.toBlocking().exchange(
                HttpRequest.GET<ByteArray>("/api/attachments/${UUID.randomUUID()}/download"),
                ByteArray::class.java
            )
        }
        ex.isFailure shouldBe true
        val status = (ex.exceptionOrNull() as? HttpClientResponseException)?.status
        status shouldBe HttpStatus.NOT_FOUND
    }

    "POST /api/attachments/upload with invalid patientId UUID returns 400" {
        val body = MultipartBody.builder()
            .addPart("file", "doc.pdf", MediaType.of("application/pdf"), "content".toByteArray())
            .addPart("patientId", "not-a-uuid")
            .addPart("uploadedBy", UUID.randomUUID().toString())
            .build()

        val ex = kotlin.runCatching {
            client.toBlocking().retrieve(
                HttpRequest.POST("/api/attachments/upload", body).contentType(MediaType.MULTIPART_FORM_DATA),
                String::class.java
            )
        }
        ex.isFailure shouldBe true
        val status = (ex.exceptionOrNull() as? HttpClientResponseException)?.status
        status shouldBe HttpStatus.BAD_REQUEST
    }
})
