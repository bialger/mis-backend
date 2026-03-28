package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import com.bialger.api.http.PaginationLinks
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api/audit-logs")
@Tag(name = "Audit", description = "Audit log (action logging)")
class AuditApiController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Audit log page")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when multiple pages exist"),
        ApiResponse(responseCode = "400", description = "Invalid pagination")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<Map<String, Any?>>> {
        val page = crmShellApplicationService.auditPage(pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }
}
