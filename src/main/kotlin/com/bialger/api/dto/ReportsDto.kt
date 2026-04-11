package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Introspected
@Schema(description = "High-level summary counts")
data class ReportsSummaryCountsDto(
    val patientsTotal: Int,
    val appointmentsTotal: Int,
    val appointmentsToday: Int,
    val employeesTotal: Int,
    val branchesTotal: Int
)

@Serdeable
@Introspected
@Schema(description = "Appointment statistics breakdown")
data class ReportsStatsDto(
    val totalPatients: Int,
    val totalAppointments: Int,
    val totalDoctors: Int,
    val onlineAppointments: Int,
    val frontDeskAppointments: Int,
    val avgAppointmentTime: Int,
    val cancelRate: Double,
    val noShowRate: Double,
    val satisfactionRate: Double?,
    val scheduleLoad: Int,
    val bookedCount: Int,
    val confirmedCount: Int,
    val canceledCount: Int,
    val noShowCount: Int,
    val totalRevenue: Double,
    val paidRevenue: Double
)

@Serdeable
@Introspected
@Schema(description = "API resource link")
data class ReportsApiLinkDto(
    val label: String,
    val path: String
)

@Serdeable
@Introspected
@Schema(description = "Reports summary response")
data class ReportsSummaryRestDto(
    val summary: ReportsSummaryCountsDto,
    val stats: ReportsStatsDto,
    val apiResourceLinks: List<ReportsApiLinkDto>
)
