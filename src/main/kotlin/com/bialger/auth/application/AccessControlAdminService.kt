package com.bialger.auth.application

import com.bialger.auth.api.dto.AccessEmployeeOverrideUpdateRequestDto
import com.bialger.auth.api.dto.AccessEmployeePermissionsDto
import com.bialger.auth.api.dto.AccessEmployeeSummaryDto
import com.bialger.auth.api.dto.AccessBackdateDaysDto
import com.bialger.auth.api.dto.AccessPermissionOverrideState
import com.bialger.auth.api.dto.AccessPermissionRowDto
import com.bialger.auth.domain.AccessPermissionCodes
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.EmployeePermissionEntity
import com.bialger.domain.core.entity.RoleEntity
import com.bialger.domain.core.repository.EmployeePermissionRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.PermissionRepository
import com.bialger.domain.core.repository.RoleRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
open class AccessControlAdminService(
    private val employeeRepository: EmployeeRepository,
    private val roleRepository: RoleRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val permissionRepository: PermissionRepository,
    private val employeePermissionRepository: EmployeePermissionRepository,
    private val accessControlService: AccessControlService
) {

    @Transactional(readOnly = true)
    open fun listEmployees(): List<AccessEmployeeSummaryDto> {
        val roleById = roleRepository.findAllOrdered().associateBy { it.id }
        return employeeRepository.findAllOrdered()
            .mapNotNull { employee ->
                if (isSysadminEmployee(employee.id, roleById)) {
                    return@mapNotNull null
                }
                val role = primaryRole(employee.id, roleById)
                AccessEmployeeSummaryDto(
                    id = employee.id.toString(),
                    fullName = employee.fullName,
                    email = employee.email,
                    roleCode = role?.name ?: "STAFF",
                    roleLabel = role?.displayName ?: role?.name ?: "Сотрудник",
                    isActive = employee.isActive
                )
            }
    }

    @Transactional(readOnly = true)
    open fun employeePermissions(employeeId: UUID): AccessEmployeePermissionsDto {
        val employee = findEmployeeOrThrow(employeeId)
        ensureSysadminPermissionsMutable(employee.id)
        val role = primaryRole(employee.id)
        val rolePermissionCodes = accessControlService.rolePermissionCodes(role?.id)
        val effectiveCodes = accessControlService.resolveEffectivePermissionCodes(employee)
        val overridesByPermissionId = employeePermissionRepository.findByEmployeeId(employee.id)
            .associateBy { it.permissionId }
        val permissionRows = permissionRepository.findAllOrdered().map { permission ->
            val overrideState = when (overridesByPermissionId[permission.id]?.isGranted) {
                true -> AccessPermissionOverrideState.GRANT
                false -> AccessPermissionOverrideState.DENY
                null -> AccessPermissionOverrideState.INHERIT
            }
            AccessPermissionRowDto(
                code = permission.code,
                name = permission.name ?: permission.code,
                description = permission.description ?: "",
                roleGranted = permission.code in rolePermissionCodes,
                overrideState = overrideState,
                effectiveGranted = permission.code in effectiveCodes
            )
        }
        return AccessEmployeePermissionsDto(
            employee = AccessEmployeeSummaryDto(
                id = employee.id.toString(),
                fullName = employee.fullName,
                email = employee.email,
                roleCode = role?.name ?: "STAFF",
                roleLabel = role?.displayName ?: role?.name ?: "Сотрудник",
                isActive = employee.isActive
            ),
            permissions = permissionRows,
            backdateDays = backdateDaysDto(employee, effectiveCodes)
        )
    }

    @Transactional
    open fun updateEmployeeOverrides(
        employeeId: UUID,
        request: AccessEmployeeOverrideUpdateRequestDto
    ): AccessEmployeePermissionsDto {
        val employee = findEmployeeOrThrow(employeeId)
        ensureSysadminPermissionsMutable(employee.id)
        val byCode = permissionRepository.findAllOrdered().associateBy { it.code }
        request.overrides.forEach { override ->
            val code = override.permissionCode.trim()
            val permission = byCode[code]
                ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "Unknown permission code: $code")
            val existing = employeePermissionRepository.findByEmployeeIdAndPermissionId(employee.id, permission.id)
            when (override.state) {
                AccessPermissionOverrideState.INHERIT -> {
                    if (existing != null) {
                        employeePermissionRepository.deleteByEmployeeIdAndPermissionId(employee.id, permission.id)
                    }
                }
                AccessPermissionOverrideState.GRANT -> upsertOverride(
                    existing = existing,
                    employeeId = employee.id,
                    permissionId = permission.id,
                    isGranted = true
                )
                AccessPermissionOverrideState.DENY -> upsertOverride(
                    existing = existing,
                    employeeId = employee.id,
                    permissionId = permission.id,
                    isGranted = false
                )
            }
        }
        return employeePermissions(employee.id)
    }

    @Transactional(readOnly = true)
    open fun employeeBackdateDays(employeeId: UUID): AccessBackdateDaysDto {
        val employee = findEmployeeOrThrow(employeeId)
        ensureSysadminPermissionsMutable(employee.id)
        val effectiveCodes = accessControlService.resolveEffectivePermissionCodes(employee)
        return backdateDaysDto(employee, effectiveCodes)
    }

    @Transactional
    open fun updateEmployeeBackdateDays(employeeId: UUID, days: Int?): AccessBackdateDaysDto {
        val employee = findEmployeeOrThrow(employeeId)
        ensureSysadminPermissionsMutable(employee.id)
        val normalized = days?.coerceAtLeast(0)
        if (employee.backdateDaysOverride != normalized) {
            employeeRepository.update(employee.copy(backdateDaysOverride = normalized))
        }
        val reloaded = findEmployeeOrThrow(employee.id)
        val effectiveCodes = accessControlService.resolveEffectivePermissionCodes(reloaded)
        return backdateDaysDto(reloaded, effectiveCodes)
    }

    private fun upsertOverride(
        existing: EmployeePermissionEntity?,
        employeeId: UUID,
        permissionId: UUID,
        isGranted: Boolean
    ) {
        if (existing == null) {
            employeePermissionRepository.save(
                EmployeePermissionEntity(
                    id = UUID.randomUUID(),
                    employeeId = employeeId,
                    permissionId = permissionId,
                    isGranted = isGranted
                )
            )
        } else if (existing.isGranted != isGranted) {
            employeePermissionRepository.update(existing.copy(isGranted = isGranted))
        }
    }

    private fun findEmployeeOrThrow(employeeId: UUID): EmployeeEntity =
        employeeRepository.findById(employeeId).orElseThrow {
            HttpStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }

    private fun ensureSysadminPermissionsMutable(employeeId: UUID) {
        if (isSysadminEmployee(employeeId)) {
            throw HttpStatusException(HttpStatus.FORBIDDEN, "SYSADMIN permissions cannot be viewed or changed")
        }
    }

    private fun isSysadminEmployee(
        employeeId: UUID,
        roleById: Map<UUID, RoleEntity> = emptyMap()
    ): Boolean =
        employeeRoleRepository.findByEmployeeId(employeeId)
            .mapNotNull { roleLink ->
                roleById[roleLink.roleId] ?: roleRepository.findById(roleLink.roleId).orElse(null)
            }
            .any { it.name == "SYSADMIN" }

    private fun primaryRole(
        employeeId: UUID,
        roleById: Map<UUID, RoleEntity> = emptyMap()
    ): RoleEntity? =
        employeeRoleRepository.findByEmployeeId(employeeId)
            .firstOrNull()
            ?.let { roleById[it.roleId] ?: roleRepository.findById(it.roleId).orElse(null) }

    private fun backdateDaysDto(employee: EmployeeEntity, effectiveCodes: Set<String>): AccessBackdateDaysDto =
        AccessBackdateDaysDto(
            globalDefaultDays = accessControlService.globalBackdateDaysDefault(),
            employeeOverrideDays = employee.backdateDaysOverride?.coerceAtLeast(0),
            effectiveDays = accessControlService.resolveBackdateDaysLimit(employee, effectiveCodes),
            canUseBackdateEditing = AccessPermissionCodes.APPOINTMENT_BACKDATE_EDIT in effectiveCodes
        )
}
