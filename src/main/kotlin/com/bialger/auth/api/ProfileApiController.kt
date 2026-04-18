package com.bialger.auth.api

import com.bialger.auth.api.dto.AuthChangePasswordRequestDto
import com.bialger.auth.api.dto.AuthProfileResponseDto
import com.bialger.auth.api.dto.AuthProfileUpdateRequestDto
import com.bialger.auth.application.CurrentUserContext
import com.bialger.auth.application.CurrentUserContextService
import com.bialger.auth.domain.PasswordHasher
import com.bialger.domain.core.repository.EmployeeRepository
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.Instant

@Controller("/api/auth")
@Tag(name = "Profile", description = "Authenticated user profile and password management")
open class ProfileApiController(
    private val currentUserContextService: CurrentUserContextService,
    private val employeeRepository: EmployeeRepository,
    private val passwordHasher: PasswordHasher
) {

    @Get("/profile", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get current authenticated user profile")
    open fun profile(): AuthProfileResponseDto = currentUserContextService.currentOrThrow().toProfileDto()

    @Patch("/profile", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update current user profile (without permissions/roles)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated profile"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "409", description = "Email is already in use")
    )
    @Transactional
    open fun updateProfile(@Body @Valid request: AuthProfileUpdateRequestDto): AuthProfileResponseDto {
        val context = currentUserContextService.currentOrThrow()
        val existing = employeeRepository.findById(context.employee.id).orElseThrow {
            HttpStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        }

        val fullName = request.fullName.trim()
        if (fullName.isEmpty()) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Full name is required")
        }
        val email = request.email?.trim()?.takeIf { it.isNotEmpty() }
        val phone = request.phone?.trim()?.takeIf { it.isNotEmpty() }

        if (email != null && email != existing.email && employeeRepository.existsByEmail(email)) {
            throw HttpStatusException(HttpStatus.CONFLICT, "Email is already in use")
        }

        val updated = existing.copy(
            fullName = fullName,
            email = email,
            phone = phone,
            updatedAt = Instant.now()
        )
        employeeRepository.update(updated)
        return currentUserContextService.resolveForEmployee(updated).toProfileDto()
    }

    @Post("/change-password", consumes = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Change current user password")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Password changed"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "401", description = "Invalid current password")
    )
    @Transactional
    open fun changePassword(@Body @Valid request: AuthChangePasswordRequestDto): HttpResponse<Any> {
        val context = currentUserContextService.currentOrThrow()
        val existing = employeeRepository.findById(context.employee.id).orElseThrow {
            HttpStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        }
        if (!passwordHasher.matches(request.currentPassword, existing.passwordHash)) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Current password is invalid")
        }

        val newPassword = request.newPassword.trim()
        if (newPassword == request.currentPassword) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "New password must differ from current password")
        }

        employeeRepository.update(
            existing.copy(
                passwordHash = passwordHasher.hash(newPassword),
                mustChangePassword = false,
                updatedAt = Instant.now()
            )
        )
        return HttpResponse.noContent()
    }

    private fun CurrentUserContext.toProfileDto(): AuthProfileResponseDto = AuthProfileResponseDto(
        id = employee.id.toString(),
        fullName = employee.fullName,
        email = employee.email,
        phone = employee.phone,
        roleCode = roleCode
    )
}
