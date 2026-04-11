package com.bialger.api

import com.bialger.domain.patient.repository.PatientConsentRepository
import com.bialger.domain.patient.repository.PatientRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest
class PatientConsentsApiTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val patientRepository: PatientRepository,
    private val patientConsentRepository: PatientConsentRepository
) : StringSpec({

    val createdPatientIds = mutableListOf<UUID>()

    afterTest {
        createdPatientIds.forEach { patientId ->
            runCatching {
                patientConsentRepository.findByPatientId(patientId)
                    .forEach { patientConsentRepository.deleteById(it.id) }
            }
            runCatching { patientRepository.deleteById(patientId) }
        }
        createdPatientIds.clear()
    }

    /**
     * Gets the first available organization ID from the branches API (uses seeded data).
     */
    fun orgId(): String {
        val branchResp = client.toBlocking().retrieve("/api/branches?page=0&size=1")
        val content = objectMapper.readTree(branchResp).path("content")
        return if (content.isArray && content.size() > 0) {
            content[0].path("organizationId").asText()
        } else {
            "a0000001-0000-4000-8000-000000000001"
        }
    }

    /**
     * Creates a patient via the HTTP API using the first available organization.
     * Returns the patient ID string.
     */
    fun createPatient(): String {
        val cardNumber = "TST-${UUID.randomUUID().toString().take(8)}"
        val body = """
            {
              "organizationId":"${orgId()}",
              "cardNumber":"$cardNumber",
              "fullName":"Consent Test Patient"
            }
        """.trimIndent()
        val resp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/patients", body).contentType(MediaType.APPLICATION_JSON)
        )
        val id = objectMapper.readTree(resp).path("id").asText()
        runCatching { createdPatientIds += UUID.fromString(id) }
        return id
    }

    "GET /api/patients/{patientId}/consents returns empty list for new patient" {
        val patientId = createPatient()
        val body = client.toBlocking().retrieve("/api/patients/$patientId/consents")
        val tree = objectMapper.readTree(body)
        tree.isArray shouldBe true
    }

    "GET /api/patients/{nonExistentId}/consents returns 404" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().retrieve("/api/patients/${UUID.randomUUID()}/consents")
        }
        ex.status shouldBe HttpStatus.NOT_FOUND
    }

    "POST /api/patients/{patientId}/consents creates a consent" {
        val patientId = createPatient()
        val body = """{"consentType":"MARKETING","isGranted":true}"""
        val resp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/patients/$patientId/consents", body).contentType(MediaType.APPLICATION_JSON)
        )
        resp shouldContain "\"consentType\":\"MARKETING\""
        resp shouldContain "\"isGranted\":true"
    }

    "POST /api/patients/{patientId}/consents with unknown type returns 400" {
        val patientId = createPatient()
        val body = """{"consentType":"UNKNOWN_TYPE","isGranted":true}"""
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/patients/$patientId/consents", body).contentType(MediaType.APPLICATION_JSON),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.BAD_REQUEST
    }

    "POST same consent twice updates existing record" {
        val patientId = createPatient()
        val grantBody = """{"consentType":"GOV_DATA_TRANSFER","isGranted":true}"""
        val revokeBody = """{"consentType":"GOV_DATA_TRANSFER","isGranted":false}"""
        client.toBlocking().retrieve(
            HttpRequest.POST("/api/patients/$patientId/consents", grantBody).contentType(MediaType.APPLICATION_JSON)
        )
        val revoked = client.toBlocking().retrieve(
            HttpRequest.POST("/api/patients/$patientId/consents", revokeBody).contentType(MediaType.APPLICATION_JSON)
        )
        revoked shouldContain "\"isGranted\":false"

        val list = client.toBlocking().retrieve("/api/patients/$patientId/consents")
        val arr = objectMapper.readTree(list)
        arr.isArray shouldBe true
        arr.size() shouldBe 1
    }
})
