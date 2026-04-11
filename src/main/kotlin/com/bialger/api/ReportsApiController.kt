package com.bialger.api

import com.bialger.api.dto.ReportsApiLinkDto
import com.bialger.api.dto.ReportsStatsDto
import com.bialger.api.dto.ReportsSummaryCountsDto
import com.bialger.api.dto.ReportsSummaryRestDto
import com.bialger.application.shell.CrmShellApplicationService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api/reports")
@Tag(name = "Reports", description = "Reports and summaries (management analytics within CRM data)")
class ReportsApiController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get("/summary", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Aggregate metrics for the reports screen")
    @ApiResponse(responseCode = "200", description = "Summary and REST resource paths",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ReportsSummaryRestDto::class))])
    fun summary(): ReportsSummaryRestDto {
        val r = crmShellApplicationService.reportsPayload()

        @Suppress("UNCHECKED_CAST")
        val summaryMap = r["summary"] as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val statsMap = r["stats"] as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val linksRaw = r["apiResourceLinks"] as? List<Map<String, String>> ?: emptyList()

        fun int(m: Map<String, Any?>, k: String) = (m[k] as? Number)?.toInt() ?: 0
        fun dbl(m: Map<String, Any?>, k: String) = (m[k] as? Number)?.toDouble() ?: 0.0

        return ReportsSummaryRestDto(
            summary = ReportsSummaryCountsDto(
                patientsTotal = int(summaryMap, "patientsTotal"),
                appointmentsTotal = int(summaryMap, "appointmentsTotal"),
                appointmentsToday = int(summaryMap, "appointmentsToday"),
                employeesTotal = int(summaryMap, "employeesTotal"),
                branchesTotal = int(summaryMap, "branchesTotal")
            ),
            stats = ReportsStatsDto(
                totalPatients = int(statsMap, "totalPatients"),
                totalAppointments = int(statsMap, "totalAppointments"),
                totalDoctors = int(statsMap, "totalDoctors"),
                onlineAppointments = int(statsMap, "onlineAppointments"),
                frontDeskAppointments = int(statsMap, "frontDeskAppointments"),
                avgAppointmentTime = int(statsMap, "avgAppointmentTime"),
                cancelRate = dbl(statsMap, "cancelRate"),
                noShowRate = dbl(statsMap, "noShowRate"),
                satisfactionRate = (statsMap["satisfactionRate"] as? Number)?.toDouble(),
                scheduleLoad = int(statsMap, "scheduleLoad"),
                bookedCount = int(statsMap, "bookedCount"),
                confirmedCount = int(statsMap, "confirmedCount"),
                canceledCount = int(statsMap, "canceledCount"),
                noShowCount = int(statsMap, "noShowCount"),
                totalRevenue = dbl(statsMap, "totalRevenue"),
                paidRevenue = dbl(statsMap, "paidRevenue")
            ),
            apiResourceLinks = linksRaw.map { m ->
                ReportsApiLinkDto(label = m["label"] ?: "", path = m["path"] ?: "")
            }
        )
    }
}
