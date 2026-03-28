package com.bialger.web

import com.bialger.application.shell.CrmShellApplicationService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import java.util.UUID
import io.swagger.v3.oas.annotations.Hidden

/**
 * Legacy bootstrap under `/mvc/shell` — same payload as [com.bialger.api.ShellBootstrapApiController].
 */
@Hidden
@Controller("/mvc/shell")
class CrmShellBootstrapController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get("/bootstrap", produces = [MediaType.APPLICATION_JSON])
    fun bootstrap(
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
