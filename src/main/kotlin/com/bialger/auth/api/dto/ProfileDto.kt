package com.bialger.auth.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Serdeable
@Introspected
@Schema(description = "Current authenticated user profile")
data class AuthProfileResponseDto(
    val id: String,
    val fullName: String,
    val email: String?,
    val phone: String?,
    val roleCode: String
)

@Serdeable
@Introspected
@Schema(description = "Update current user profile")
data class AuthProfileUpdateRequestDto(
    @field:NotBlank @field:Size(max = 255) val fullName: String,
    @field:Email @field:Size(max = 255) val email: String? = null,
    @field:Size(max = 50) val phone: String? = null
)

@Serdeable
@Introspected
@Schema(description = "Change current user password")
data class AuthChangePasswordRequestDto(
    @field:NotBlank val currentPassword: String,
    @field:NotBlank @field:Size(min = 8, max = 255) val newPassword: String
)
