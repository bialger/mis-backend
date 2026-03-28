package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

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
