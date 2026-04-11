package com.bialger.api

import com.bialger.domain.attachment.repository.AttachmentRepository
import com.bialger.domain.core.repository.EmployeeRepository
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
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest
class AttachmentApiTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val attachmentRepository: AttachmentRepository,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val storageStub: StorageServiceStub
) : StringSpec({

    val createdAttachmentIds = mutableListOf<UUID>()
    val createdPatientIds = mutableListOf<UUID>()
    val createdEmployeeIds = mutableListOf<UUID>()

    /**
     * Returns the first available role ID from /api/catalog/roles (seeded by V15).
     */
    fun roleId(): String {
        val resp = client.toBlocking().retrieve("/api/catalog/roles")
        val arr = objectMapper.readTree(resp)
        return arr[0].path("id").asText()
    }

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
     * Creates an employee via HTTP API (ensures the record is committed before further requests).
     */
    fun createEmployee(): String {
        val suffix = UUID.randomUUID().toString().take(8)
        val body = """
            {
              "fullName": "AttachUploader-$suffix",
              "email": "uploader-$suffix@test.mis",
              "password": "Test1234!",
              "isActive": true,
              "roleId": "${roleId()}"
            }
        """.trimIndent()
        val resp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/employees", body).contentType(MediaType.APPLICATION_JSON)
        )
        val id = objectMapper.readTree(resp).path("id").asText()
        runCatching { createdEmployeeIds += UUID.fromString(id) }
        return id
    }

    afterTest {
        storageStub.reset()
        createdAttachmentIds.forEach { runCatching { attachmentRepository.deleteById(it) } }
        createdAttachmentIds.clear()
        createdPatientIds.forEach { runCatching { patientRepository.deleteById(it) } }
        createdPatientIds.clear()
        createdEmployeeIds.forEach { runCatching { employeeRepository.deleteById(it) } }
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
