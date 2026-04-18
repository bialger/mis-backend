package com.bialger.auth.application

import com.bialger.api.dto.MeRestDto
import com.bialger.api.dto.MeUserDto
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeBranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.RoleRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.utils.SecurityService
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.util.UUID

data class CurrentUserContext(
    val employee: EmployeeEntity,
    val roleCode: String,
    val branchScope: List<String>,
    val permissions: Map<String, Any>
) {
    fun toMeDto(): MeRestDto = MeRestDto(
        user = MeUserDto(
            id = employee.id.toString(),
            name = employee.fullName,
            role = roleCode,
            login = employee.email ?: employee.id.toString()
        ),
        branchScope = branchScope,
        permissions = permissions
    )
}

@Singleton
open class CurrentUserContextService(
    private val securityService: SecurityService,
    private val employeeRepository: EmployeeRepository,
    private val roleRepository: RoleRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val branchRepository: BranchRepository,
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val accessControlService: AccessControlService
) {

    @Transactional(readOnly = true)
    open fun currentOrThrow(): CurrentUserContext =
        resolve(securityService.authentication.orElse(null))
            ?: throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")

    @Transactional(readOnly = true)
    open fun resolve(authentication: Authentication?): CurrentUserContext? {
        if (authentication == null) return null
        val employee = findEmployee(authentication) ?: return null
        if (!employee.isActive) return null
        return buildContext(employee)
    }

    @Transactional(readOnly = true)
    open fun resolveForEmployee(employee: EmployeeEntity): CurrentUserContext {
        require(employee.isActive) { "Employee is inactive" }
        return buildContext(employee)
    }

    private fun findEmployee(authentication: Authentication): EmployeeEntity? {
        val employeeId = authentication.attributes["employeeId"]?.toString()
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (employeeId != null) {
            val byId = employeeRepository.findById(employeeId).orElse(null)
            if (byId != null) return byId
        }
        return employeeRepository.findByEmail(authentication.name)
    }

    private fun buildContext(employee: EmployeeEntity): CurrentUserContext {
        val roleId = employeeRoleRepository.findByEmployeeId(employee.id).firstOrNull()?.roleId
        val roleCode = roleId?.let { roleRepository.findById(it).orElse(null)?.name } ?: "STAFF"
        val permissionCodes = accessControlService.resolveEffectivePermissionCodes(employee)
        val branchIds = employeeBranchRepository.findByEmployeeId(employee.id)
            .map { it.branchId.toString() }
            .distinct()
            .ifEmpty { branchRepository.findAllOrdered().map { it.id.toString() } }
        val permissions = accessControlService.resolveLegacyPermissionMap(employee, permissionCodes)
        return CurrentUserContext(
            employee = employee,
            roleCode = roleCode,
            branchScope = branchIds,
            permissions = permissions
        )
    }
}
