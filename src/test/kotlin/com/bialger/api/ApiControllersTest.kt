package com.bialger.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
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
class ApiControllersTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper
) : StringSpec({

    "GET /api/me returns stub user and permissions" {
        val body = client.toBlocking().retrieve("/api/me")
        body shouldContain "\"user\""
        body shouldContain "permissions"
    }

    "GET /api/shell/bootstrap returns dashboard payload" {
        val body = client.toBlocking().retrieve("/api/shell/bootstrap?kind=dashboard")
        objectMapper.readTree(body).path("kind").asText() shouldBe "dashboard"
    }

    "GET /api/shell/bootstrap with unknown kind returns 400" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().retrieve("/api/shell/bootstrap?kind=__unknown_kind__")
        }
        ex.status shouldBe HttpStatus.BAD_REQUEST
    }

    "GET /api/patients returns paged JSON" {
        val body = client.toBlocking().retrieve("/api/patients?page=0&size=10")
        objectMapper.readTree(body).path("content").isArray shouldBe true
    }

    "GET /api/patients/{id} for missing id returns 404" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().retrieve("/api/patients/${UUID.randomUUID()}")
        }
        ex.status shouldBe HttpStatus.NOT_FOUND
    }

    "GET /api/patients/{id}/tags for missing patient returns 404" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().retrieve("/api/patients/${UUID.randomUUID()}/tags")
        }
        ex.status shouldBe HttpStatus.NOT_FOUND
    }

    "POST /api/patients with invalid bean validation returns 400" {
        val body =
            """{"organizationId":"00000000-0000-0000-0000-000000000001","cardNumber":"","fullName":"Name"}"""
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/patients", body).contentType(MediaType.APPLICATION_JSON),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.BAD_REQUEST
        val err = ex.response.getBody(String::class.java).orElse("")
        err shouldContain "validation_failed"
        err shouldContain "cardNumber"
    }

    "GET /api/patients sets Link header when a next page exists" {
        val body = client.toBlocking().retrieve("/api/patients?page=0&size=1")
        val total = objectMapper.readTree(body).path("totalSize").asLong()
        if (total > 1L) {
            val resp = client.toBlocking().exchange(
                HttpRequest.GET<Any>("/api/patients?page=0&size=1"),
                String::class.java
            )
            resp.header("Link").shouldNotBeNull()
            resp.header("Link") shouldContain "rel=\"next\""
        }
    }

    "GET /api/audit-logs returns paged JSON" {
        val body = client.toBlocking().retrieve("/api/audit-logs?page=0&size=5")
        objectMapper.readTree(body).path("content").isArray shouldBe true
    }

    "GET /api/branches returns paged JSON" {
        val body = client.toBlocking().retrieve("/api/branches?page=0&size=50")
        objectMapper.readTree(body).path("content").isArray shouldBe true
    }
})
