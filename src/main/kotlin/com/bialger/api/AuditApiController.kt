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
@Tag(name = "Audit", description = "Audit log (action logging); supports filtering by entity, date, and employee")
class AuditApiController(
    private val auditLogRepository: AuditLogRepository,
    private val employeeRepository: EmployeeRepository
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "List audit log entries with optional filters",
        description = "Filterable by entityType, entityId, employeeId, dateFrom, dateTo. Defaults to last 90 days."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of audit entries; Link header when multiple pages",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AuditLogEntryDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid pagination or date format")
    )
    fun list(
        @QueryValue(defaultValue = "") entityType: String,
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
            entityType.isNotBlank() ->
                auditLogRepository.findByEntityTypeAndDateRange(entityType, from, to, pageable)
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
    @Operation(summary = "Audit log for medical records (reads and writes)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of MEDICAL_RECORD audit entries",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AuditLogEntryDto::class))])
    )
    fun medicalRecordsLog(
        @QueryValue entityId: UUID?,
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

        val page = if (entityId != null) {
            auditLogRepository.findByEntityTypeAndEntityIdAndDateRange("MEDICAL_RECORD", entityId, from, to, pageable)
        } else {
            auditLogRepository.findByEntityTypeAndDateRange("MEDICAL_RECORD", from, to, pageable)
        }

        val mapped = page.map { e -> toDto(e, employees[e.employeeId]?.fullName) }
        val resp = HttpResponse.ok(mapped)
        PaginationLinks.appendToResponse(request, mapped, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get single audit log entry by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Audit log entry",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AuditLogEntryDto::class))]),
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
            timestamp = ts,
            ts = ts,
            diff = AuditDiffDto(old = e.oldValue, new = e.newValue)
        )
    }
}
