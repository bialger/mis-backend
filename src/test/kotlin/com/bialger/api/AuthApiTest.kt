package com.bialger.api

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.auth.domain.PasswordHasher
import com.bialger.support.TestAuthHeaders
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
import io.micronaut.transaction.SynchronousTransactionManager
import java.sql.Connection
import java.time.Instant
import java.util.UUID

@MicronautTest(transactional = false)
class AuthApiTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val employeeRepository: EmployeeRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val roleRepository: RoleRepository,
    private val passwordHasher: PasswordHasher,
    private val transactionManager: SynchronousTransactionManager<Connection>
) : StringSpec({
    val createdEmployeeIds = mutableSetOf<UUID>()

    afterTest {
        transactionManager.executeWrite {
            createdEmployeeIds.forEach { id ->
                runCatching { employeeRoleRepository.deleteByEmployeeId(id) }
                runCatching { employeeRepository.deleteById(id) }
            }
        }
        createdEmployeeIds.clear()
    }

    fun createLoginEmployee(
        login: String,
        password: String,
        active: Boolean,
        mustChangePassword: Boolean = false
    ): UUID = transactionManager.executeWrite {
        employeeRepository.findByEmail(login)?.let { existing ->
            runCatching { employeeRoleRepository.deleteByEmployeeId(existing.id) }
            runCatching { employeeRepository.deleteById(existing.id) }
        }
        val id = UUID.randomUUID()
        val employee = EmployeeEntity(
            id = id,
            fullName = "Auth Test ${id.toString().take(8)}",
            email = login,
            passwordHash = passwordHasher.hash(password),
            mustChangePassword = mustChangePassword,
            isActive = active,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        employeeRepository.save(employee)
        val role = roleRepository.findByName("SYSADMIN")
            ?: roleRepository.findAllOrdered().firstOrNull()
            ?: throw IllegalStateException("Role not found")
        employeeRoleRepository.save(id, role.id)
        createdEmployeeIds += id
        id
    }

    fun loginRequest(login: String, password: String): HttpRequest<String> =
        HttpRequest.POST(
            "/api/auth/login",
            """{"login":"$login","password":"$password"}"""
        )
            .contentType(MediaType.APPLICATION_JSON)
            .header(TestAuthHeaders.NO_AUTH, "1")

    "valid login returns 200 with Set-Cookie and bearer token in JSON" {
        val login = "auth-ok-${UUID.randomUUID()}@mis.local"
        val password = "StrongPass123!"
        createLoginEmployee(login, password, active = true)

        val response = client.toBlocking().exchange(loginRequest(login, password), String::class.java)
        response.status shouldBe HttpStatus.OK
        val setCookie = response.header("Set-Cookie").shouldNotBeNull()
        setCookie shouldContain "MIS_AUTH="
        val tree = objectMapper.readTree(response.body())
        tree.path("token").asText().isNotBlank() shouldBe true
        tree.path("user").path("id").asText().isNotBlank() shouldBe true
    }

    "logout clears auth cookie" {
        val login = "auth-logout-${UUID.randomUUID()}@mis.local"
        val password = "StrongPass123!"
        createLoginEmployee(login, password, active = true)

        val loginResponse = client.toBlocking().exchange(loginRequest(login, password), String::class.java)
        val token = objectMapper.readTree(loginResponse.body()).path("token").asText()
        token.isNotBlank() shouldBe true

        val logoutResponse = client.toBlocking().exchange(
            HttpRequest.POST("/api/auth/logout", "")
                .header(TestAuthHeaders.NO_AUTH, "1")
                .header("Authorization", "Bearer $token"),
            String::class.java
        )
        logoutResponse.status shouldBe HttpStatus.NO_CONTENT
        val setCookie = logoutResponse.header("Set-Cookie").shouldNotBeNull()
        setCookie shouldContain "MIS_AUTH="
        setCookie shouldContain "Max-Age=0"
    }

    "invalid password returns 401" {
        val login = "auth-bad-pass-${UUID.randomUUID()}@mis.local"
        createLoginEmployee(login, "StrongPass123!", active = true)

        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(loginRequest(login, "wrong-pass"), String::class.java)
        }
        ex.status shouldBe HttpStatus.UNAUTHORIZED
    }

    "unknown login returns 401" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                loginRequest("missing-${UUID.randomUUID()}@mis.local", "StrongPass123!"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.UNAUTHORIZED
    }

    "inactive user returns 401" {
        val login = "auth-inactive-${UUID.randomUUID()}@mis.local"
        val password = "StrongPass123!"
        createLoginEmployee(login, password, active = false)

        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(loginRequest(login, password), String::class.java)
        }
        ex.status shouldBe HttpStatus.UNAUTHORIZED
    }

    "sysadmin with mustChangePassword=true gets passwordChangeRequired response" {
        val login = "auth-sysadmin-rotate-${UUID.randomUUID()}@mis.local"
        val password = "StrongPass123!"
        createLoginEmployee(login, password, active = true, mustChangePassword = true)

        val response = client.toBlocking().exchange(loginRequest(login, password), String::class.java)
        response.status shouldBe HttpStatus.OK
        response.header("Set-Cookie") shouldBe null
        val tree = objectMapper.readTree(response.body())
        tree.path("passwordChangeRequired").asBoolean() shouldBe true
        tree.path("token").asText("").isBlank() shouldBe true
    }

    "first-password-change returns token and cookie" {
        val login = "auth-first-change-${UUID.randomUUID()}@mis.local"
        val oldPassword = "StrongPass123!"
        val newPassword = "StrongerPass456!"
        createLoginEmployee(login, oldPassword, active = true, mustChangePassword = true)

        val response = client.toBlocking().exchange(
            HttpRequest.POST(
                "/api/auth/first-password-change",
                """{"login":"$login","password":"$oldPassword","newPassword":"$newPassword"}"""
            )
                .contentType(MediaType.APPLICATION_JSON)
                .header(TestAuthHeaders.NO_AUTH, "1"),
            String::class.java
        )
        response.status shouldBe HttpStatus.OK
        val setCookie = response.header("Set-Cookie").shouldNotBeNull()
        setCookie shouldContain "MIS_AUTH="
        val tree = objectMapper.readTree(response.body())
        tree.path("passwordChangeRequired").asBoolean() shouldBe false
        tree.path("token").asText().isNotBlank() shouldBe true
    }

    "first-password-change for non-required account returns 409" {
        val login = "auth-no-first-change-${UUID.randomUUID()}@mis.local"
        val password = "StrongPass123!"
        createLoginEmployee(login, password, active = true, mustChangePassword = false)

        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST(
                    "/api/auth/first-password-change",
                    """{"login":"$login","password":"$password","newPassword":"AnotherPass123!"}"""
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.CONFLICT
    }

    "empty body returns 400" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/login", "{}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.BAD_REQUEST
    }

    "broken json body returns 400" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/login", "{\"login\":\"bad\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.BAD_REQUEST
    }
})
