package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api/reports")
@Tag(name = "Reports", description = "Reports and summaries (management analytics within CRM data)")
class ReportsApiController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get("/summary", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Aggregate metrics for the reports screen")
    @ApiResponse(responseCode = "200", description = "Summary and REST resource paths")
    fun summary(): Map<String, Any?> {
        val r = crmShellApplicationService.reportsPayload()
        return mapOf(
            "summary" to r["summary"],
            "stats" to r["stats"],
            "apiResourceLinks" to r["apiResourceLinks"]
        )
    }
}
