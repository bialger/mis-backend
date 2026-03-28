package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID

@Controller("/api/shell")
@Tag(name = "Shell", description = "Начальное состояние SPA (совместимо с SSE refresh)")
class ShellBootstrapApiController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get("/bootstrap", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Bootstrap JSON для страницы CRM")
    @ApiResponse(responseCode = "200", description = "Payload страницы")
    @ApiResponse(responseCode = "400", description = "Неверные параметры")
    fun bootstrap(
        @Parameter(description = "dashboard|patients|patient-detail|doctors|schedule|…")
        @QueryValue kind: String,
        @QueryValue patientId: String?,
        @QueryValue appointmentId: String?
    ): HttpResponse<Map<String, Any?>> {
        val pid = patientId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val aid = appointmentId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        return try {
            val payload = crmShellApplicationService.bootstrapPayload(kind, pid, aid)
            HttpResponse.ok(payload)
        } catch (_: IllegalArgumentException) {
            HttpResponse.badRequest()
        }
    }
}
