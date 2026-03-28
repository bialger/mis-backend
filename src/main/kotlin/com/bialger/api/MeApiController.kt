package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api")
@Tag(name = "Session", description = "Текущий пользователь (заглушка)")
class MeApiController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Hidden
    @Get("/me", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Текущий пользователь и права (без реальной авторизации)")
    fun me(): Map<String, Any?> = crmShellApplicationService.mePayload()
}
