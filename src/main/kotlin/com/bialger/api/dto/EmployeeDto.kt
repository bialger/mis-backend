package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Employee response")
data class EmployeeRestDto(
    val id: String,
    val fullName: String,
    val email: String?,
    val phone: String?,
    val isActive: Boolean,
    val login: String,
    /** Alias for fullName — kept for SPA backward compatibility. */
    val name: String,
    /** Role UUID (primary key of the role entity). */
    val role: String,
    val roleId: String,
    /** Role code name, e.g. "HEAD", "DOCTOR". */
    val roleCode: String,
    /** Role display label, e.g. "Главврач". */
    val roleLabel: String,
    val specialtyIds: List<String>,
    val branchIds: List<String>,
    val branchScope: List<String>
)

@Serdeable
@Introspected
@Schema(description = "Create employee (staff)")
data class EmployeeCreateDto(
    @field:NotBlank val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    @field:NotBlank val password: String,
    val isActive: Boolean = true,
    val specialtyIds: List<UUID> = emptyList(),
    val branchIds: List<UUID> = emptyList(),
    @field:NotNull val roleId: UUID
)

@Serdeable
@Introspected
@Schema(description = "Update employee")
data class EmployeeUpdateDto(
    @field:NotBlank val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val isActive: Boolean = true,
    val specialtyIds: List<UUID> = emptyList(),
    val branchIds: List<UUID> = emptyList(),
    @field:NotNull val roleId: UUID
)
