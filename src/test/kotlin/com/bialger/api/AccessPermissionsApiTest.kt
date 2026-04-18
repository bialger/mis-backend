package com.bialger.api

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeePermissionRepository
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
class AccessPermissionsApiTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val employeeRepository: EmployeeRepository,
    private val employeePermissionRepository: EmployeePermissionRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val roleRepository: RoleRepository,
    private val passwordHasher: PasswordHasher,
    private val transactionManager: SynchronousTransactionManager<Connection>
) : StringSpec({
    val createdEmployeeIds = mutableSetOf<UUID>()

    afterTest {
        transactionManager.executeWrite {
            createdEmployeeIds.forEach { id ->
                runCatching { employeePermissionRepository.deleteByEmployeeId(id) }
                runCatching { employeeRoleRepository.deleteByEmployeeId(id) }
                runCatching { employeeRepository.deleteById(id) }
            }
        }
        createdEmployeeIds.clear()
    }

    fun createEmployee(roleCode: String, mustChangePassword: Boolean = false): Pair<UUID, String> =
        transactionManager.executeWrite {
            val id = UUID.randomUUID()
            val login = "acl-${roleCode.lowercase()}-${id.toString().take(8)}@mis.local"
            val password = "StrongPass123!"
            val employee = EmployeeEntity(
                id = id,
                fullName = "ACL $roleCode ${id.toString().take(6)}",
                email = login,
                phone = null,
                passwordHash = passwordHasher.hash(password),
                mustChangePassword = mustChangePassword,
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            employeeRepository.save(employee)
            val role = roleRepository.findByName(roleCode)
                ?: throw IllegalStateException("Role $roleCode not found")
            employeeRoleRepository.save(id, role.id)
            createdEmployeeIds += id
            id to password
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

    fun authed(path: String, token: String): HttpRequest<Any> =
        HttpRequest.GET<Any>(path)
            .header(TestAuthHeaders.NO_AUTH, "1")
            .header("Authorization", "Bearer $token")

    "head has read-only access to organizations and permissions" {
        val (headId) = createEmployee("HEAD")
        val head = employeeRepository.findById(headId).orElseThrow()
        val token = loginToken(head.email ?: error("email required"), "StrongPass123!")

        val orgRead = client.toBlocking().exchange(
            authed("/api/organizations?page=0&size=1", token),
            String::class.java
        )
        orgRead.status shouldBe HttpStatus.OK

        val orgWriteEx = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/organizations", """{"name":"ORG-${UUID.randomUUID()}"}""")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1")
                    .header("Authorization", "Bearer $token"),
                String::class.java
            )
        }
        orgWriteEx.status shouldBe HttpStatus.FORBIDDEN

        val accessRead = client.toBlocking().exchange(
            authed("/api/access/employees", token),
            String::class.java
        )
        accessRead.status shouldBe HttpStatus.OK

        val accessWriteEx = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.PUT(
                    "/api/access/employees/${head.id}/permissions",
                    """{"overrides":[{"permissionCode":"patients.view","state":"GRANT"}]}"""
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1")
                    .header("Authorization", "Bearer $token"),
                String::class.java
            )
        }
        accessWriteEx.status shouldBe HttpStatus.FORBIDDEN
    }

    "sysadmin can manage non-sysadmin overrides but cannot view or edit sysadmin ACL rows" {
        val (sysadminId) = createEmployee("SYSADMIN")
        val (doctorId) = createEmployee("DOCTOR")
        val sysadmin = employeeRepository.findById(sysadminId).orElseThrow()
        val doctor = employeeRepository.findById(doctorId).orElseThrow()
        val token = loginToken(sysadmin.email ?: error("email required"), "StrongPass123!")

        val shell = client.toBlocking().exchange(
            authed("/api/shell/bootstrap?kind=settings", token),
            String::class.java
        )
        shell.status shouldBe HttpStatus.OK

        val patientsForbidden = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                authed("/api/patients?page=0&size=1", token),
                String::class.java
            )
        }
        patientsForbidden.status shouldBe HttpStatus.FORBIDDEN

        val employeesResponse = client.toBlocking().exchange(
            authed("/api/access/employees", token),
            String::class.java
        )
        employeesResponse.status shouldBe HttpStatus.OK
        val employeesTree = objectMapper.readTree(employeesResponse.body())
        employeesTree.any { it.path("id").asText() == sysadmin.id.toString() } shouldBe false
        employeesTree.any { it.path("id").asText() == doctor.id.toString() } shouldBe true

        val updatedDoctor = client.toBlocking().exchange(
            HttpRequest.PUT(
                "/api/access/employees/${doctor.id}/permissions",
                """{"overrides":[{"permissionCode":"finance.view","state":"GRANT"}]}"""
            )
                .contentType(MediaType.APPLICATION_JSON)
                .header(TestAuthHeaders.NO_AUTH, "1")
                .header("Authorization", "Bearer $token"),
            String::class.java
        )
        updatedDoctor.status shouldBe HttpStatus.OK
        val updatedTree = objectMapper.readTree(updatedDoctor.body())
        val financeRow = updatedTree.path("permissions")
            .firstOrNull { it.path("code").asText() == "finance.view" }
            ?: error("finance.view row not found")
        financeRow.path("effectiveGranted").asBoolean() shouldBe true

        val backdateUpdated = client.toBlocking().exchange(
            HttpRequest.PUT(
                "/api/access/employees/${doctor.id}/backdate-days",
                """{"days":14}"""
            )
                .contentType(MediaType.APPLICATION_JSON)
                .header(TestAuthHeaders.NO_AUTH, "1")
                .header("Authorization", "Bearer $token"),
            String::class.java
        )
        backdateUpdated.status shouldBe HttpStatus.OK
        val backdateTree = objectMapper.readTree(backdateUpdated.body())
        backdateTree.path("employeeOverrideDays").asInt() shouldBe 14
        backdateTree.path("effectiveDays").asInt() shouldBe 14

        val selfUpdateEx = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.PUT(
                    "/api/access/employees/${sysadmin.id}/permissions",
                    """{"overrides":[{"permissionCode":"patients.view","state":"GRANT"}]}"""
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1")
                    .header("Authorization", "Bearer $token"),
                String::class.java
            )
        }
        selfUpdateEx.status shouldBe HttpStatus.FORBIDDEN

        val selfBackdateEx = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.PUT(
                    "/api/access/employees/${sysadmin.id}/backdate-days",
                    """{"days":30}"""
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1")
                    .header("Authorization", "Bearer $token"),
                String::class.java
            )
        }
        selfBackdateEx.status shouldBe HttpStatus.FORBIDDEN
    }
})
