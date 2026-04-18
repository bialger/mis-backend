package com.bialger.auth.application

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeePermissionRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.PermissionRepository
import com.bialger.domain.core.repository.RolePermissionRepository
import com.bialger.domain.system.repository.SystemSettingRepository
import com.bialger.auth.domain.AccessPermissionCodes
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
open class AccessControlService(
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val permissionRepository: PermissionRepository,
    private val employeePermissionRepository: EmployeePermissionRepository,
    private val systemSettingRepository: SystemSettingRepository
) {

    @Transactional(readOnly = true)
    open fun resolveEffectivePermissionCodes(employee: EmployeeEntity): Set<String> {
        val roleIds = employeeRoleRepository.findByEmployeeId(employee.id).map { it.roleId }.distinct()
        val permissionById = permissionRepository.findAllOrdered().associateBy { it.id }
        val roleCodes = roleIds
            .flatMap { rid -> rolePermissionRepository.findByRoleId(rid) }
            .mapNotNull { permissionById[it.permissionId]?.code }
            .toMutableSet()

        val overrides = employeePermissionRepository.findByEmployeeId(employee.id)
        for (override in overrides) {
            val code = permissionById[override.permissionId]?.code ?: continue
            if (override.isGranted) roleCodes += code else roleCodes -= code
        }
        return roleCodes
    }

    @Transactional(readOnly = true)
    open fun resolveLegacyPermissionMap(employee: EmployeeEntity, effectiveCodes: Set<String>): Map<String, Any> {
        return mapOf(
            "canViewDashboard" to (AccessPermissionCodes.DASHBOARD_VIEW in effectiveCodes),
            "canViewPosts" to (AccessPermissionCodes.POSTS_VIEW in effectiveCodes),
            "canViewPatients" to (AccessPermissionCodes.PATIENTS_VIEW in effectiveCodes),
            "canViewDoctors" to (AccessPermissionCodes.DOCTORS_VIEW in effectiveCodes),
            "canViewSchedule" to (AccessPermissionCodes.SCHEDULE_VIEW in effectiveCodes),
            "canViewAppointments" to (AccessPermissionCodes.APPOINTMENTS_VIEW in effectiveCodes),
            "canViewReports" to (AccessPermissionCodes.REPORTS_VIEW in effectiveCodes),
            "canViewAudit" to (AccessPermissionCodes.AUDIT_VIEW in effectiveCodes),
            "canViewSettings" to (AccessPermissionCodes.SETTINGS_VIEW in effectiveCodes),

            "canReadEmployees" to (AccessPermissionCodes.EMPLOYEE_READ in effectiveCodes),
            "canManageEmployees" to (AccessPermissionCodes.EMPLOYEE_WRITE in effectiveCodes),
            "canReadBranches" to (AccessPermissionCodes.BRANCH_READ in effectiveCodes),
            "canManageBranches" to (AccessPermissionCodes.BRANCH_WRITE in effectiveCodes),
            "canReadOrganizations" to (AccessPermissionCodes.ORGANIZATION_READ in effectiveCodes),
            "canManageOrganizations" to (AccessPermissionCodes.ORGANIZATION_WRITE in effectiveCodes),
            "canReadPermissions" to (AccessPermissionCodes.PERMISSION_READ in effectiveCodes),
            "canManagePermissions" to (AccessPermissionCodes.PERMISSION_WRITE in effectiveCodes),
            "canManageSystemSettings" to (AccessPermissionCodes.SETTINGS_WRITE in effectiveCodes),

            "canViewFinance" to (AccessPermissionCodes.FINANCE_VIEW in effectiveCodes),
            "canEditFinance" to (AccessPermissionCodes.FINANCE_WRITE in effectiveCodes),
            "canViewInventory" to (AccessPermissionCodes.INVENTORY_VIEW in effectiveCodes),
            "canWriteInventory" to (AccessPermissionCodes.INVENTORY_WRITE in effectiveCodes),
            "canManualEgiszSend" to (AccessPermissionCodes.MANUAL_EGISZ_SEND in effectiveCodes),
            "canEditBackdateDays" to resolveBackdateDaysLimit(employee, effectiveCodes)
        )
    }

    @Transactional(readOnly = true)
    open fun resolveBackdateDaysLimit(employee: EmployeeEntity, effectiveCodes: Set<String>): Int {
        val canBackdate = AccessPermissionCodes.APPOINTMENT_BACKDATE_EDIT in effectiveCodes
        if (!canBackdate) return 0
        return employee.backdateDaysOverride?.coerceAtLeast(0) ?: globalBackdateDaysDefault()
    }

    @Transactional(readOnly = true)
    open fun globalBackdateDaysDefault(): Int {
        val raw = systemSettingRepository.findByBranchIdAndKey(null, "canEditBackdateDays")
            ?.value
            ?.trim()
        return raw?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    @Transactional(readOnly = true)
    open fun canAccessHtmlPath(path: String, permissions: Map<String, Any>): Boolean {
        val key = htmlPermissionKey(path) ?: return true
        return permissions.bool(key)
    }

    @Transactional(readOnly = true)
    open fun requiredApiPermission(path: String, method: String, shellKind: String?): String? {
        if (path.startsWith("/api/auth/")) return null
        if (path.startsWith("/api/public/booking/")) return null

        val upper = method.uppercase()
        if (path == "/api/shell/bootstrap") {
            return when (shellKind?.trim()) {
                "dashboard" -> AccessPermissionCodes.DASHBOARD_VIEW
                "patients", "patient-detail" -> AccessPermissionCodes.PATIENTS_VIEW
                "doctors" -> AccessPermissionCodes.DOCTORS_VIEW
                "schedule" -> AccessPermissionCodes.SCHEDULE_VIEW
                "inventory" -> AccessPermissionCodes.INVENTORY_VIEW
                "settings" -> AccessPermissionCodes.SETTINGS_VIEW
                "appointments-list", "appointment-detail" -> AccessPermissionCodes.APPOINTMENTS_VIEW
                "reports" -> AccessPermissionCodes.REPORTS_VIEW
                "audit" -> AccessPermissionCodes.AUDIT_VIEW
                else -> null
            }
        }

        if (path == "/api/access" || path.startsWith("/api/access/")) {
            return if (upper == "GET") AccessPermissionCodes.PERMISSION_READ else AccessPermissionCodes.PERMISSION_WRITE
        }
        if (path == "/api/organizations" || path.startsWith("/api/organizations/")) {
            return if (upper == "GET") AccessPermissionCodes.ORGANIZATION_READ else AccessPermissionCodes.ORGANIZATION_WRITE
        }
        if (path == "/api/branches" || path.startsWith("/api/branches/")) {
            return if (upper == "GET") AccessPermissionCodes.BRANCH_READ else AccessPermissionCodes.BRANCH_WRITE
        }
        if (path == "/api/employees" || path.startsWith("/api/employees/")) {
            return if (upper == "GET") AccessPermissionCodes.EMPLOYEE_READ else AccessPermissionCodes.EMPLOYEE_WRITE
        }
        if (path == "/api/system-settings" || path.startsWith("/api/system-settings/")) {
            return if (upper == "GET") AccessPermissionCodes.SETTINGS_VIEW else AccessPermissionCodes.SETTINGS_WRITE
        }
        if (path.startsWith("/api/catalog/")) {
            return AccessPermissionCodes.SETTINGS_VIEW
        }
        if (path == "/api/rooms" || path.startsWith("/api/rooms/")) {
            return AccessPermissionCodes.SCHEDULE_VIEW
        }
        if (path.startsWith("/api/audit")) {
            return AccessPermissionCodes.AUDIT_VIEW
        }
        if (path.startsWith("/api/reports")) {
            return AccessPermissionCodes.REPORTS_VIEW
        }
        if (path.startsWith("/api/inventory")) {
            return if (upper == "GET") AccessPermissionCodes.INVENTORY_VIEW else AccessPermissionCodes.INVENTORY_WRITE
        }
        if (path.startsWith("/api/payments")) {
            return if (upper == "GET") AccessPermissionCodes.FINANCE_VIEW else AccessPermissionCodes.FINANCE_WRITE
        }
        if (path.startsWith("/api/time-slots")) {
            return AccessPermissionCodes.SCHEDULE_VIEW
        }
        if (path.startsWith("/api/appointments")) {
            return AccessPermissionCodes.APPOINTMENTS_VIEW
        }
        if (path.startsWith("/api/patients") ||
            path.startsWith("/api/patient-consents") ||
            path.startsWith("/api/medical-records") ||
            path.startsWith("/api/attachments")
        ) {
            return AccessPermissionCodes.PATIENTS_VIEW
        }
        return null
    }

    @Transactional(readOnly = true)
    open fun htmlPermissionKey(path: String): String? = when {
        path == "/" -> "canViewDashboard"
        path == "/posts" || path.startsWith("/posts/") -> "canViewPosts"
        path == "/patients" || path.startsWith("/patients/") -> "canViewPatients"
        path == "/doctors" || path.startsWith("/doctors/") -> "canViewDoctors"
        path == "/schedule" || path.startsWith("/schedule/") -> "canViewSchedule"
        path == "/reports" || path.startsWith("/reports/") -> "canViewReports"
        path == "/inventory" || path.startsWith("/inventory/") -> "canViewInventory"
        path == "/audit" || path.startsWith("/audit/") -> "canViewAudit"
        path == "/appointments" || path.startsWith("/appointments/") -> "canViewAppointments"
        path == "/settings" || path.startsWith("/settings/") -> "canViewSettings"
        else -> null
    }

    @Transactional(readOnly = true)
    open fun rolePermissionCodes(roleId: UUID?): Set<String> {
        if (roleId == null) return emptySet()
        val permissionById = permissionRepository.findAllOrdered().associateBy { it.id }
        return rolePermissionRepository.findByRoleId(roleId)
            .mapNotNull { permissionById[it.permissionId]?.code }
            .toSet()
    }

    private fun Map<String, Any>.bool(key: String): Boolean = this[key] as? Boolean == true
}
