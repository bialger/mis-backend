package com.bialger.auth.api.dto

import com.bialger.api.dto.MeUserDto
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Serdeable
@Introspected
@Schema(description = "Login request")
data class AuthLoginRequestDto(
    @field:NotBlank @field:Email val login: String,
    @field:NotBlank val password: String
)

@Serdeable
@Introspected
@Schema(description = "Login response with JWT token and resolved user context")
data class AuthLoginResponseDto(
    val token: String? = null,
    val user: MeUserDto? = null,
    val branchScope: List<String> = emptyList(),
    val permissions: Map<String, Any> = emptyMap(),
    val passwordChangeRequired: Boolean = false
)

@Serdeable
@Introspected
@Schema(description = "Complete first-login password rotation")
data class AuthFirstPasswordChangeRequestDto(
    @field:NotBlank @field:Email val login: String,
    @field:NotBlank val password: String,
    @field:NotBlank @field:Size(min = 8, max = 255) val newPassword: String
)
