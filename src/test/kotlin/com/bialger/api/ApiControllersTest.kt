package com.bialger.api

import com.bialger.support.TestAuthHeaders
import com.bialger.support.TestAuthSupport
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.token.generator.TokenGenerator
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
@Property(name = "micronaut.http.client.follow-redirects", value = "false")
@Property(name = "JWT_SECRET", value = "test-jwt-secret-for-integration-tests-only-1234567890")
class ApiControllersTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val tokenGenerator: TokenGenerator,
    private val testAuthSupport: TestAuthSupport
) : StringSpec({

    beforeSpec {
        // Prime JWT + DB user before any HTTP call (avoids races with parallel specs / first-request quirks).
        testAuthSupport.session()
    }

    "authenticated GET /api/shell/bootstrap returns dashboard payload" {
        val body = client.toBlocking().retrieve("/api/shell/bootstrap?kind=dashboard")
        objectMapper.readTree(body).path("kind").asText() shouldBe "dashboard"
    }

    "authenticated GET /api/shell/bootstrap with unknown kind returns 400" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().retrieve("/api/shell/bootstrap?kind=__unknown_kind__")
        }
        ex.status shouldBe HttpStatus.BAD_REQUEST
    }

    "authenticated GET /api/patients returns paged JSON" {
        val body = client.toBlocking().retrieve("/api/patients?page=0&size=10")
        objectMapper.readTree(body).path("content").isArray shouldBe true
    }

    "authenticated GET /api/patients works with cookie-only JWT" {
        val token = testAuthSupport.session().token
        val body = client.toBlocking().retrieve(
            HttpRequest.GET<Any>("/api/patients?page=0&size=10")
                .header(TestAuthHeaders.NO_AUTH, "1")
                .header("Cookie", "MIS_AUTH=$token")
        )
        objectMapper.readTree(body).path("content").isArray shouldBe true
    }

    "authenticated POST /graphql succeeds" {
        val payload = """{"query":"query { patients(page:0,size:1){ pageInfo { size } } }"}"""
        val response = client.toBlocking().exchange(
            HttpRequest.POST("/graphql", payload).contentType(MediaType.APPLICATION_JSON),
            String::class.java
        )
        response.status shouldBe HttpStatus.OK
        response.body().shouldContain("data")
    }

    "authenticated GET / returns internal page content" {
        val html = client.toBlocking().retrieve("/")
        html shouldContain "Панель управления"
    }

    "authenticated non-sysadmin GET /grafana returns 403" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.GET<Any>("/grafana"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.FORBIDDEN
    }

    "authenticated non-sysadmin GET /api/grafana/** returns 403" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.GET<Any>("/api/grafana/status"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.FORBIDDEN
    }

    "unauthenticated GET /api/patients returns 401" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.GET<Any>("/api/patients?page=0&size=1")
                    .header(TestAuthHeaders.NO_AUTH, "1"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.UNAUTHORIZED
    }

    "unauthenticated POST /graphql returns 401" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST(
                    "/graphql",
                    """{"query":"query { patients(page:0,size:1){ pageInfo { size } } }"}"""
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.UNAUTHORIZED
    }

    "unauthenticated GET / redirects to /login" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/")
                .header(TestAuthHeaders.NO_AUTH, "1"),
            String::class.java
        )
        response.status shouldBe HttpStatus.SEE_OTHER
        response.header("Location").shouldNotBeNull()
        response.header("Location") shouldBe "/login"
    }

    "invalid JWT returns 401" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.GET<Any>("/api/patients?page=0&size=1")
                    .header(TestAuthHeaders.NO_AUTH, "1")
                    .header("Authorization", "Bearer broken.jwt.token"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.UNAUTHORIZED
    }

    "expired JWT returns 401" {
        val employee = testAuthSupport.integrationEmployee()
        val auth = Authentication.build(
            employee.email ?: TestAuthSupport.TEST_LOGIN,
            listOf("SYSADMIN"),
            mapOf(
                "employeeId" to employee.id.toString(),
                "role" to "SYSADMIN"
            )
        )
        val expired = tokenGenerator.generateToken(auth, -1)
            .orElseThrow { IllegalStateException("Could not generate expired token") }

        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.GET<Any>("/api/patients?page=0&size=1")
                    .header(TestAuthHeaders.NO_AUTH, "1")
                    .header("Authorization", "Bearer $expired"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.UNAUTHORIZED
    }

    "GET /api/me with JWT returns 404" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().retrieve("/api/me")
        }
        ex.status shouldBe HttpStatus.NOT_FOUND
    }
})
