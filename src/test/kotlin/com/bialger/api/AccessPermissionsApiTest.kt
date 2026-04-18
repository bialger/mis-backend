package com.bialger.api

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.EmployeePermissionEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeePermissionRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.PermissionRepository
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
    private val permissionRepository: PermissionRepository,
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

    fun upsertPermissionOverride(employeeId: UUID, permissionCode: String, isGranted: Boolean) {
        transactionManager.executeWrite {
            val permission = permissionRepository.findByCode(permissionCode)
                ?: error("Permission $permissionCode not found")
            val existing = employeePermissionRepository.findByEmployeeIdAndPermissionId(employeeId, permission.id)
            if (existing == null) {
                employeePermissionRepository.save(
                    EmployeePermissionEntity(
                        id = UUID.randomUUID(),
                        employeeId = employeeId,
                        permissionId = permission.id,
                        isGranted = isGranted
                    )
                )
            } else if (existing.isGranted != isGranted) {
                employeePermissionRepository.update(existing.copy(isGranted = isGranted))
            }
            Unit
        }
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

        val backdateWriteEx = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.PUT(
                    "/api/access/employees/${head.id}/backdate-days",
                    """{"days":7}"""
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1")
                    .header("Authorization", "Bearer $token"),
                String::class.java
            )
        }
        backdateWriteEx.status shouldBe HttpStatus.FORBIDDEN
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
        val backdateTree = updatedTree.path("backdateDays")
        backdateTree.path("roleCode").asText() shouldBe "DOCTOR"
        backdateTree.path("roleDefaultDays").asInt() shouldBe 60
        (backdateTree.path("employeeOverrideDays").isNull || backdateTree.path("employeeOverrideDays").isMissingNode) shouldBe true
        backdateTree.path("effectiveDays").asInt() shouldBe 60

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
        val backdateUpdatedTree = objectMapper.readTree(backdateUpdated.body())
        backdateUpdatedTree.path("roleCode").asText() shouldBe "DOCTOR"
        backdateUpdatedTree.path("roleDefaultDays").asInt() shouldBe 60
        backdateUpdatedTree.path("employeeOverrideDays").asInt() shouldBe 14
        backdateUpdatedTree.path("effectiveDays").asInt() shouldBe 14

        val backdateInherited = client.toBlocking().exchange(
            HttpRequest.PUT(
                "/api/access/employees/${doctor.id}/backdate-days",
                """{"days":null}"""
            )
                .contentType(MediaType.APPLICATION_JSON)
                .header(TestAuthHeaders.NO_AUTH, "1")
                .header("Authorization", "Bearer $token"),
            String::class.java
        )
        backdateInherited.status shouldBe HttpStatus.OK
        val backdateInheritedTree = objectMapper.readTree(backdateInherited.body())
        (backdateInheritedTree.path("employeeOverrideDays").isNull || backdateInheritedTree.path("employeeOverrideDays").isMissingNode) shouldBe true
        backdateInheritedTree.path("effectiveDays").asInt() shouldBe 60

        val denyBackdate = client.toBlocking().exchange(
            HttpRequest.PUT(
                "/api/access/employees/${doctor.id}/permissions",
                """{"overrides":[{"permissionCode":"appointments.backdate.edit","state":"DENY"}]}"""
            )
                .contentType(MediaType.APPLICATION_JSON)
                .header(TestAuthHeaders.NO_AUTH, "1")
                .header("Authorization", "Bearer $token"),
            String::class.java
        )
        denyBackdate.status shouldBe HttpStatus.OK
        val denyBackdateTree = objectMapper.readTree(denyBackdate.body()).path("backdateDays")
        denyBackdateTree.path("roleCode").asText() shouldBe "DOCTOR"
        denyBackdateTree.path("roleDefaultDays").asInt() shouldBe 60
        (denyBackdateTree.path("employeeOverrideDays").isNull || denyBackdateTree.path("employeeOverrideDays").isMissingNode) shouldBe true
        denyBackdateTree.path("effectiveDays").asInt() shouldBe 0
        denyBackdateTree.path("canUseBackdateEditing").asBoolean() shouldBe false

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

    "non privileged roles cannot see ACL screens even with explicit permission override" {
        val (adminId) = createEmployee("ADMIN")
        val admin = employeeRepository.findById(adminId).orElseThrow()
        upsertPermissionOverride(admin.id, "permission.read", true)
        upsertPermissionOverride(admin.id, "permission.write", true)
        val token = loginToken(admin.email ?: error("email required"), "StrongPass123!")

        val settingsShell = client.toBlocking().exchange(
            authed("/api/shell/bootstrap?kind=settings", token),
            String::class.java
        )
        settingsShell.status shouldBe HttpStatus.OK
        val sections = objectMapper.readTree(settingsShell.body()).path("sections")
        sections.any { it.path("id").asText() == "access-permissions" } shouldBe false

        val readAclForbidden = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                authed("/api/access/employees", token),
                String::class.java
            )
        }
        readAclForbidden.status shouldBe HttpStatus.FORBIDDEN

        val writeAclForbidden = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.PUT(
                    "/api/access/employees/${admin.id}/permissions",
                    """{"overrides":[{"permissionCode":"patients.view","state":"GRANT"}]}"""
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TestAuthHeaders.NO_AUTH, "1")
                    .header("Authorization", "Bearer $token"),
                String::class.java
            )
        }
        writeAclForbidden.status shouldBe HttpStatus.FORBIDDEN
    }
})
