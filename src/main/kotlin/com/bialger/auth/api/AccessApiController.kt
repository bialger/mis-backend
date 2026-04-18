package com.bialger.auth.api

import com.bialger.auth.api.dto.AccessEmployeeOverrideUpdateRequestDto
import com.bialger.auth.api.dto.AccessEmployeePermissionsDto
import com.bialger.auth.api.dto.AccessEmployeeSummaryDto
import com.bialger.auth.api.dto.AccessBackdateDaysDto
import com.bialger.auth.api.dto.AccessBackdateDaysUpdateRequestDto
import com.bialger.auth.application.AccessControlAdminService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/access")
@Tag(name = "Access", description = "Account-level access control")
open class AccessApiController(
    private val accessControlAdminService: AccessControlAdminService
) {

    @Get("/employees", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List employees for access-control management")
    open fun employees(): List<AccessEmployeeSummaryDto> =
        accessControlAdminService.listEmployees()

    @Get("/employees/{employeeId}/permissions", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get effective permissions for employee (role + overrides)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Permissions payload"),
        ApiResponse(responseCode = "404", description = "Employee not found")
    )
    open fun employeePermissions(@PathVariable employeeId: UUID): AccessEmployeePermissionsDto =
        accessControlAdminService.employeePermissions(employeeId)

    @Put("/employees/{employeeId}/permissions", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Upsert account-level overrides for employee")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated permissions payload"),
        ApiResponse(responseCode = "400", description = "Unknown permission code or invalid payload"),
        ApiResponse(responseCode = "404", description = "Employee not found")
    )
    open fun updateEmployeePermissions(
        @PathVariable employeeId: UUID,
        @Body @Valid request: AccessEmployeeOverrideUpdateRequestDto
    ): AccessEmployeePermissionsDto =
        accessControlAdminService.updateEmployeeOverrides(employeeId, request)

    @Put("/employees/{employeeId}/backdate-days", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Set employee-level backdate days override (null = inherit role default)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated backdate-days payload"),
        ApiResponse(responseCode = "400", description = "Invalid payload"),
        ApiResponse(responseCode = "404", description = "Employee not found")
    )
    open fun updateEmployeeBackdateDays(
        @PathVariable employeeId: UUID,
        @Body @Valid request: AccessBackdateDaysUpdateRequestDto
    ): AccessBackdateDaysDto =
        accessControlAdminService.updateEmployeeBackdateDays(employeeId, request.days)
}
