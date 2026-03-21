package com.bialger.web

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import java.util.UUID

/**
 * JSON with the same payload as [CrmShellPageData] for the shell page,
 * so SSE can refresh content without a full reload (not a REST CRUD API).
 */
@Controller("/mvc/shell")
class CrmShellBootstrapController(
    private val crmShellPageData: CrmShellPageData
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
            val payload = crmShellPageData.bootstrapPayload(kind, pid, aid)
            HttpResponse.ok(payload)
        } catch (_: IllegalArgumentException) {
            HttpResponse.badRequest()
        }
    }
}
