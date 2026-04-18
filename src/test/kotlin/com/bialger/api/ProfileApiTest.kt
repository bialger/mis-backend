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
import io.kotest.matchers.shouldBe
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
class ProfileApiTest(
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

    fun createEmployee(email: String, password: String): UUID = transactionManager.executeWrite {
        val id = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(
                id = id,
                fullName = "Profile ${id.toString().take(8)}",
                email = email,
                phone = "79990000000",
                passwordHash = passwordHasher.hash(password),
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val role = roleRepository.findByName("DOCTOR")
            ?: roleRepository.findAllOrdered().firstOrNull()
            ?: error("Role not found")
        employeeRoleRepository.save(id, role.id)
        createdEmployeeIds += id
        id
    }

    fun loginToken(email: String, password: String): String {
        val response = client.toBlocking().exchange(
            HttpRequest.POST(
                "/api/auth/login",
                """{"login":"$email","password":"$password"}"""
            )
                .contentType(MediaType.APPLICATION_JSON)
                .header(TestAuthHeaders.NO_AUTH, "1"),
            String::class.java
        )
        response.status shouldBe HttpStatus.OK
        return objectMapper.readTree(response.body()).path("token").asText()
    }

    fun authed(method: String, path: String, token: String, body: String? = null): HttpRequest<String> {
        val request = when (method) {
            "GET" -> HttpRequest.GET<String>(path)
            "PATCH" -> HttpRequest.PATCH(path, body ?: "")
            "POST" -> HttpRequest.POST(path, body ?: "")
            else -> error("Unsupported method $method")
        }
        return request
            .header(TestAuthHeaders.NO_AUTH, "1")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
    }

    "profile endpoints allow updating own email and password but not rights" {
        val email = "profile-${UUID.randomUUID()}@mis.local"
        val oldPassword = "StrongPass123!"
        val newPassword = "StrongerPass456!"
        createEmployee(email, oldPassword)
        val token = loginToken(email, oldPassword)

        val profileResponse = client.toBlocking().exchange(
            authed("GET", "/api/auth/profile", token),
            String::class.java
        )
        profileResponse.status shouldBe HttpStatus.OK
        val profileTree = objectMapper.readTree(profileResponse.body())
        profileTree.path("email").asText() shouldBe email

        val updatedEmail = "profile-updated-${UUID.randomUUID()}@mis.local"
        val updateResponse = client.toBlocking().exchange(
            authed(
                "PATCH",
                "/api/auth/profile",
                token,
                """{"fullName":"Updated Doctor","email":"$updatedEmail","phone":"79991112233"}"""
            ),
            String::class.java
        )
        updateResponse.status shouldBe HttpStatus.OK
        val updateTree = objectMapper.readTree(updateResponse.body())
        updateTree.path("fullName").asText() shouldBe "Updated Doctor"
        updateTree.path("email").asText() shouldBe updatedEmail

        val wrongCurrentEx = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                authed(
                    "POST",
                    "/api/auth/change-password",
                    token,
                    """{"currentPassword":"bad-pass","newPassword":"$newPassword"}"""
                ),
                String::class.java
            )
        }
        wrongCurrentEx.status shouldBe HttpStatus.UNAUTHORIZED

        val passwordResponse = client.toBlocking().exchange(
            authed(
                "POST",
                "/api/auth/change-password",
                token,
                """{"currentPassword":"$oldPassword","newPassword":"$newPassword"}"""
            ),
            String::class.java
        )
        passwordResponse.status shouldBe HttpStatus.NO_CONTENT

        shouldThrow<HttpClientResponseException> {
            loginToken(updatedEmail, oldPassword)
        }.status shouldBe HttpStatus.UNAUTHORIZED

        val newToken = loginToken(updatedEmail, newPassword)
        newToken.isNotBlank() shouldBe true
    }

    "profile update rejects duplicate email" {
        val firstEmail = "profile-first-${UUID.randomUUID()}@mis.local"
        val secondEmail = "profile-second-${UUID.randomUUID()}@mis.local"
        createEmployee(firstEmail, "StrongPass123!")
        createEmployee(secondEmail, "StrongPass123!")
        val token = loginToken(firstEmail, "StrongPass123!")

        val duplicateEx = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                authed(
                    "PATCH",
                    "/api/auth/profile",
                    token,
                    """{"fullName":"Doctor One","email":"$secondEmail","phone":"79990000001"}"""
                ),
                String::class.java
            )
        }
        duplicateEx.status shouldBe HttpStatus.CONFLICT
    }
})
