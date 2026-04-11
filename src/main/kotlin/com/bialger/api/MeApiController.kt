package com.bialger.api

import com.bialger.api.dto.MeRestDto
import com.bialger.application.shell.CrmShellApplicationService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api")
@Tag(name = "Session", description = "Session: current user and permissions (stub until JWT auth)")
class MeApiController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get("/me", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Current user and permissions (stub until authentication is implemented)")
    @ApiResponse(responseCode = "200", description = "User, branch scope, permissions",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = MeRestDto::class))])
    fun me(): MeRestDto = crmShellApplicationService.mePayload()
}
