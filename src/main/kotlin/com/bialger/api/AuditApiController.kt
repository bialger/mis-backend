package com.bialger.api

import com.bialger.api.dto.AuditDiffDto
import com.bialger.api.dto.AuditLogEntryDto
import com.bialger.api.http.PaginationLinks
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.repository.AuditLogRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Controller("/api/audit-logs")
@Tag(name = "Audit", description = "Audit log (action logging). Access restricted to SYSADMIN. Supports filtering by entity, action, date, and employee.")
class AuditApiController(
    private val auditLogRepository: AuditLogRepository,
    private val employeeRepository: EmployeeRepository
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "List audit log entries with optional filters",
        description = "Filterable by entityType, action, entityId, employeeId, dateFrom, dateTo. Defaults to last 90 days. **SYSADMIN only.**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of audit entries; Link header when multiple pages",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AuditLogEntryDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid pagination or date format"),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Forbidden — SYSADMIN role required")
    )
    fun list(
        @Parameter(description = "Filter by entity type, e.g. MEDICAL_RECORD")
        @QueryValue(defaultValue = "") entityType: String,
        @Parameter(description = "Filter by action, e.g. CREATED, UPDATED, DELETED, READ, ACCESS_DENIED")
        @QueryValue(defaultValue = "") action: String,
        @QueryValue entityId: UUID?,
        @QueryValue employeeId: UUID?,
        @QueryValue dateFrom: String?,
        @QueryValue dateTo: String?,
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<AuditLogEntryDto>> {
        val employees = employeeRepository.findAllOrdered().associateBy { it.id }

        val from = dateFrom?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Instant.now().minus(90, ChronoUnit.DAYS)
        val to = dateTo?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Instant.now()

        val page: Page<AuditLogEntity> = when {
            entityType.isNotBlank() && entityId != null ->
                auditLogRepository.findByEntityTypeAndEntityIdAndDateRange(entityType, entityId, from, to, pageable)
            entityType.isNotBlank() && action.isNotBlank() ->
                auditLogRepository.findByEntityTypeAndActionAndDateRange(entityType, action, from, to, pageable)
            entityType.isNotBlank() ->
                auditLogRepository.findByEntityTypeAndDateRange(entityType, from, to, pageable)
            action.isNotBlank() ->
                auditLogRepository.findByActionAndDateRange(action, from, to, pageable)
            employeeId != null ->
                auditLogRepository.findByEmployeeId(employeeId, pageable)
            else ->
                auditLogRepository.findByDateRange(from, to, pageable)
        }

        val mapped = page.map { e -> toDto(e, employees[e.employeeId]?.fullName) }
        val resp = HttpResponse.ok(mapped)
        PaginationLinks.appendToResponse(request, mapped, resp)
        return resp
    }

    @Get("/medical-records", produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "Audit log for medical records (reads, writes, deletions, and access denials)",
        description = "Returns audit entries for the MEDICAL_RECORD entity type. **SYSADMIN only.**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of MEDICAL_RECORD audit entries",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AuditLogEntryDto::class))]),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Forbidden — SYSADMIN role required")
    )
    fun medicalRecordsLog(
        @Parameter(description = "Filter by specific record UUID")
        @QueryValue entityId: UUID?,
        @Parameter(description = "Filter by action, e.g. CREATED, UPDATED, DELETED, READ, ACCESS_DENIED")
        @QueryValue(defaultValue = "") action: String,
        @QueryValue dateFrom: String?,
        @QueryValue dateTo: String?,
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<AuditLogEntryDto>> {
        val employees = employeeRepository.findAllOrdered().associateBy { it.id }
        val from = dateFrom?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Instant.now().minus(90, ChronoUnit.DAYS)
        val to = dateTo?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Instant.now()

        val page = when {
            entityId != null ->
                auditLogRepository.findByEntityTypeAndEntityIdAndDateRange("MEDICAL_RECORD", entityId, from, to, pageable)
            action.isNotBlank() ->
                auditLogRepository.findByEntityTypeAndActionAndDateRange("MEDICAL_RECORD", action, from, to, pageable)
            else ->
                auditLogRepository.findByEntityTypeAndDateRange("MEDICAL_RECORD", from, to, pageable)
        }

        val mapped = page.map { e -> toDto(e, employees[e.employeeId]?.fullName) }
        val resp = HttpResponse.ok(mapped)
        PaginationLinks.appendToResponse(request, mapped, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "Get single audit log entry by id",
        description = "**SYSADMIN only.**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Audit log entry",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AuditLogEntryDto::class))]),
        ApiResponse(responseCode = "401", description = "Not authenticated"),
        ApiResponse(responseCode = "403", description = "Forbidden — SYSADMIN role required"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): AuditLogEntryDto {
        val employees = employeeRepository.findAllOrdered().associateBy { it.id }
        val e = auditLogRepository.findById(id).orElseThrow {
            io.micronaut.http.exceptions.HttpStatusException(io.micronaut.http.HttpStatus.NOT_FOUND, "Not found")
        }
        return toDto(e, employees[e.employeeId]?.fullName)
    }

    private fun toDto(e: AuditLogEntity, employeeName: String?): AuditLogEntryDto {
        val ts = e.timestamp.toString()
        return AuditLogEntryDto(
            id = e.id.toString(),
            employeeId = e.employeeId.toString(),
            userId = e.employeeId.toString(),
            employeeName = employeeName ?: e.employeeId.toString(),
            action = e.action,
            entityType = e.entityType,
            entityId = e.entityId?.toString(),
            oldValue = e.oldValue,
            newValue = e.newValue,
            ipAddress = e.ipAddress,
            userAgent = e.userAgent,
            timestamp = ts,
            ts = ts,
            diff = AuditDiffDto(old = e.oldValue, new = e.newValue)
        )
    }
}
