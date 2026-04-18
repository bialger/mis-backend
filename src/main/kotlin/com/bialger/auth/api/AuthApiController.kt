package com.bialger.auth.api

import com.bialger.auth.api.dto.AuthLoginRequestDto
import com.bialger.auth.api.dto.AuthLoginResponseDto
import com.bialger.auth.api.dto.AuthFirstPasswordChangeRequestDto
import com.bialger.auth.application.CurrentUserContextService
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.auth.domain.JwtTokenService
import com.bialger.auth.domain.PasswordHasher
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.micronaut.transaction.annotation.Transactional
import jakarta.validation.Valid
import java.time.Instant

@Controller("/api/auth")
@Tag(name = "Auth", description = "JWT authentication")
open class AuthApiController(
    private val employeeRepository: EmployeeRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenService: JwtTokenService,
    private val currentUserContextService: CurrentUserContextService
) {

    @Post("/login", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Authenticate user by employee email and password")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Authenticated; JWT in response body and HttpOnly cookie"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "401", description = "Invalid credentials or inactive user")
    )
    @Transactional(readOnly = true)
    open fun login(@Body @Valid dto: AuthLoginRequestDto): HttpResponse<AuthLoginResponseDto> {
        val login = dto.login.trim()
        val password = dto.password
        val employee = employeeRepository.findByEmail(login)
            ?: throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid login or password")
        if (!employee.isActive) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Inactive user")
        }
        if (!passwordHasher.matches(password, employee.passwordHash)) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid login or password")
        }
        val context = currentUserContextService.resolveForEmployee(employee)
        if (context.roleCode == SYSADMIN_ROLE && employee.mustChangePassword) {
            return HttpResponse.ok(
                AuthLoginResponseDto(
                    passwordChangeRequired = true
                )
            )
        }
        val token = jwtTokenService.generateToken(employee, context.roleCode)
        val payload = AuthLoginResponseDto(
            token = token,
            user = context.toMeDto().user,
            branchScope = context.branchScope,
            permissions = context.permissions,
            passwordChangeRequired = false
        )
        return HttpResponse.ok(payload).cookie(jwtTokenService.authCookie(token))
    }

    @Post("/first-password-change", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Complete first-login password change and sign in")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Password changed; JWT in response body and HttpOnly cookie"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "401", description = "Invalid credentials or inactive user"),
        ApiResponse(responseCode = "409", description = "First-login password change is not required")
    )
    @Transactional
    open fun firstPasswordChange(@Body @Valid dto: AuthFirstPasswordChangeRequestDto): HttpResponse<AuthLoginResponseDto> {
        val login = dto.login.trim()
        val currentPassword = dto.password
        val newPassword = dto.newPassword.trim()
        if (newPassword == currentPassword) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "New password must differ from the current password")
        }

        val employee = employeeRepository.findByEmail(login)
            ?: throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid login or password")
        if (!employee.isActive) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Inactive user")
        }
        if (!passwordHasher.matches(currentPassword, employee.passwordHash)) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid login or password")
        }

        val context = currentUserContextService.resolveForEmployee(employee)
        if (context.roleCode != SYSADMIN_ROLE || !employee.mustChangePassword) {
            throw HttpStatusException(HttpStatus.CONFLICT, "First-login password change is not required")
        }

        val updatedEmployee = employee.copy(
            passwordHash = passwordHasher.hash(newPassword),
            mustChangePassword = false,
            updatedAt = Instant.now()
        )
        employeeRepository.update(updatedEmployee)

        val updatedContext = currentUserContextService.resolveForEmployee(updatedEmployee)
        val token = jwtTokenService.generateToken(updatedEmployee, updatedContext.roleCode)
        val payload = AuthLoginResponseDto(
            token = token,
            user = updatedContext.toMeDto().user,
            branchScope = updatedContext.branchScope,
            permissions = updatedContext.permissions,
            passwordChangeRequired = false
        )
        return HttpResponse.ok(payload).cookie(jwtTokenService.authCookie(token))
    }

    @Post("/logout")
    @Operation(summary = "Clear JWT auth cookie")
    @ApiResponse(responseCode = "204", description = "Cookie cleared")
    open fun logout(): HttpResponse<Any> =
        HttpResponse.noContent<Any>().cookie(jwtTokenService.clearCookie())

    private companion object {
        const val SYSADMIN_ROLE = "SYSADMIN"
    }
}
