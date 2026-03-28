package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api/reports")
@Tag(name = "Reports", description = "Отчёты и сводки")
class ReportsApiController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get("/summary", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Сводные показатели для экрана отчётов")
    fun summary(): Map<String, Any?> {
        val r = crmShellApplicationService.reportsPayload()
        return mapOf(
            "summary" to r["summary"],
            "mvcLinks" to r["mvcLinks"]
        )
    }
}
