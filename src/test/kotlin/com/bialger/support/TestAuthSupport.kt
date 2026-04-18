package com.bialger.support

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.EmployeePermissionEntity
import com.bialger.domain.core.repository.EmployeePermissionRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.PermissionRepository
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.auth.domain.JwtTokenService
import com.bialger.auth.domain.PasswordHasher
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpHeaders
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.ClientFilterChain
import io.micronaut.http.filter.HttpClientFilter
import io.micronaut.transaction.SynchronousTransactionManager
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import java.sql.Connection
import java.time.Instant
import java.util.UUID

data class TestAuthSession(
    val token: String
)

object TestAuthHeaders {
    const val NO_AUTH = "X-Test-No-Auth"
}

@Singleton
@Requires(property = "test.auto-auth.enabled", value = "true", defaultValue = "true")
class TestAuthSupport(
    private val employeeRepository: EmployeeRepository,
    private val roleRepository: RoleRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val permissionRepository: PermissionRepository,
    private val employeePermissionRepository: EmployeePermissionRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenService: JwtTokenService,
    private val transactionManager: SynchronousTransactionManager<Connection>
) {
    @Volatile
    private var cachedSession: TestAuthSession? = null

    fun session(): TestAuthSession {
        cachedSession?.let { cached ->
            if (integrationEmployeeExists()) return cached
        }
        synchronized(this) {
            cachedSession?.let { cached ->
                if (integrationEmployeeExists()) return cached
                cachedSession = null
            }
            val token = transactionManager.executeWrite {
                val employee = ensureEmployee()
                val roleCode = ensureRole(employee.id)
                ensureAllPermissions(employee.id)
                jwtTokenService.generateToken(employee, roleCode)
            }
            return TestAuthSession(token = token).also { cachedSession = it }
        }
    }

    /**
     * Ensures the auto-auth integration user exists and returns it (same identity as [session] tokens).
     */
    fun integrationEmployee(): EmployeeEntity {
        session()
        return transactionManager.executeWrite {
            employeeRepository.findByEmail(TEST_LOGIN)
        } ?: error("Test auth user missing: $TEST_LOGIN")
    }

    private fun integrationEmployeeExists(): Boolean =
        transactionManager.executeWrite {
            employeeRepository.findByEmail(TEST_LOGIN) != null
        }

    fun authorize(request: MutableHttpRequest<*>): MutableHttpRequest<*> {
        if (request.headers.contains(TestAuthHeaders.NO_AUTH)) {
            request.headers.remove(TestAuthHeaders.NO_AUTH)
            return request
        }
        val token = session().token
        if (!request.headers.contains(HttpHeaders.AUTHORIZATION)) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }
        if (!request.headers.contains(HttpHeaders.COOKIE)) {
            request.header(HttpHeaders.COOKIE, "MIS_AUTH=$token")
        }
        return request
    }

    private fun ensureEmployee(): EmployeeEntity {
        val now = Instant.now()
        val existing = employeeRepository.findByEmail(TEST_LOGIN)
        val desiredHash = passwordHasher.hash(TEST_PASSWORD)
        return if (existing == null) {
            employeeRepository.save(
                EmployeeEntity(
                    id = UUID.randomUUID(),
                    fullName = "Test Auth User",
                    email = TEST_LOGIN,
                    phone = null,
                    passwordHash = desiredHash,
                    mustChangePassword = false,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            employeeRepository.update(
                existing.copy(
                    fullName = existing.fullName.ifBlank { "Test Auth User" },
                    passwordHash = desiredHash,
                    mustChangePassword = false,
                    isActive = true,
                    updatedAt = now
                )
            )
        }
    }

    private fun ensureRole(employeeId: UUID): String {
        val role = roleRepository.findByName("ADMIN")
            ?: roleRepository.findByName("SYSADMIN")
            ?: roleRepository.findAllOrdered().firstOrNull()
            ?: throw IllegalStateException("No roles found in DB for test authentication")
        val links = employeeRoleRepository.findByEmployeeId(employeeId)
        if (links.none { it.roleId == role.id }) {
            employeeRoleRepository.deleteByEmployeeId(employeeId)
            employeeRoleRepository.save(employeeId, role.id)
        }
        return role.name
    }

    private fun ensureAllPermissions(employeeId: UUID) {
        val byPermission = employeePermissionRepository.findByEmployeeId(employeeId)
            .associateBy { it.permissionId }
        permissionRepository.findAllOrdered().forEach { permission ->
            val existing = byPermission[permission.id]
            if (existing == null) {
                employeePermissionRepository.save(
                    EmployeePermissionEntity(
                        id = UUID.randomUUID(),
                        employeeId = employeeId,
                        permissionId = permission.id,
                        isGranted = true
                    )
                )
            } else if (!existing.isGranted) {
                employeePermissionRepository.update(existing.copy(isGranted = true))
            }
        }
    }

    companion object {
        const val TEST_LOGIN = "integration-auth@mis.local"
        const val TEST_PASSWORD = "IntegrationPass123!"
    }
}

@Filter("/**")
@Requires(property = "test.auto-auth.enabled", value = "true", defaultValue = "true")
class TestAutoAuthClientFilter(
    private val testAuthSupport: TestAuthSupport
) : HttpClientFilter {

    override fun doFilter(
        request: MutableHttpRequest<*>,
        chain: ClientFilterChain
    ): Publisher<out io.micronaut.http.HttpResponse<*>> {
        if (shouldSkip(request.path)) {
            return chain.proceed(request)
        }
        return chain.proceed(testAuthSupport.authorize(request))
    }

    private fun shouldSkip(path: String): Boolean {
        if (path == "/login" || path == "/patient-booking") return true
        if (path.startsWith("/assets/")) return true
        if (path.startsWith("/api/public/booking/")) return true
        return false
    }
}
