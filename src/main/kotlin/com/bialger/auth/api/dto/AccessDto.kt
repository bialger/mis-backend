package com.bialger.auth.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Serdeable
@Introspected
@Schema(description = "Employee short info for access-control settings")
data class AccessEmployeeSummaryDto(
    val id: String,
    val fullName: String,
    val email: String? = null,
    val roleCode: String,
    val roleLabel: String,
    val isActive: Boolean
)

@Serdeable
@Introspected
@Schema(description = "One permission row for account-level access control")
data class AccessPermissionRowDto(
    val code: String,
    val name: String,
    val description: String,
    val roleGranted: Boolean,
    val overrideState: AccessPermissionOverrideState,
    val effectiveGranted: Boolean
)

@Serdeable
@Introspected
@Schema(description = "Access-control details for a concrete employee account")
data class AccessEmployeePermissionsDto(
    val employee: AccessEmployeeSummaryDto,
    val permissions: List<AccessPermissionRowDto>,
    val backdateDays: AccessBackdateDaysDto
)

@Serdeable
@Introspected
@Schema(description = "Per-account limit for backdate editing")
data class AccessBackdateDaysDto(
    val roleCode: String,
    val roleDefaultDays: Int,
    val employeeOverrideDays: Int? = null,
    val effectiveDays: Int,
    val canUseBackdateEditing: Boolean
)

@Serdeable
@Introspected
@Schema(description = "Update employee-level backdate days override. null = inherit from role")
data class AccessBackdateDaysUpdateRequestDto(
    @field:Min(0) val days: Int? = null
)

@Serdeable
@Introspected
enum class AccessPermissionOverrideState {
    INHERIT,
    GRANT,
    DENY
}

@Serdeable
@Introspected
@Schema(description = "Single account-level override command")
data class AccessPermissionOverrideUpsertDto(
    @field:NotBlank val permissionCode: String,
    val state: AccessPermissionOverrideState
)

@Serdeable
@Introspected
@Schema(description = "Batch of account-level overrides for one employee")
data class AccessEmployeeOverrideUpdateRequestDto(
    @field:NotEmpty val overrides: List<@Valid AccessPermissionOverrideUpsertDto>
)
