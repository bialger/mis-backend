package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import com.bialger.api.dto.AppointmentCreateDto
import com.bialger.api.dto.AppointmentUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/appointments")
@Tag(name = "Appointments", description = "Appointments (schedule and visits)")
open class AppointmentsApiController(
    private val appointmentMvcService: AppointmentMvcService,
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List appointments")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when multiple pages exist"),
        ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<Map<String, Any?>>> {
        val rows = crmShellApplicationService.listAppointmentMaps()
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get appointment by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Appointment"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): Map<String, Any?> =
        crmShellApplicationService.appointmentMapById(id)
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create appointment")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created appointment"),
        ApiResponse(responseCode = "400", description = "Validation or business rule error"),
        ApiResponse(responseCode = "500", description = "Could not build response after create")
    )
    open fun create(@Body @Valid dto: AppointmentCreateDto): Map<String, Any?> {
        val e = appointmentMvcService.create(
            patientId = dto.patientId,
            employeeId = dto.employeeId,
            timeSlotId = dto.timeSlotId,
            branchId = dto.branchId,
            roomId = dto.roomId,
            status = AppointmentMvcService.parseStatus(dto.status),
            source = AppointmentMvcService.parseSource(dto.source),
            notes = dto.notes,
            createdBy = dto.createdBy
        )
        return crmShellApplicationService.appointmentMapById(e.id)
            ?: throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load appointment")
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update appointment")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated appointment"),
        ApiResponse(responseCode = "400", description = "Validation or business error"),
        ApiResponse(responseCode = "404", description = "Appointment not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: AppointmentUpdateDto): Map<String, Any?> {
        appointmentMvcService.update(
            id = id,
            patientId = dto.patientId,
            employeeId = dto.employeeId,
            timeSlotId = dto.timeSlotId,
            branchId = dto.branchId,
            roomId = dto.roomId,
            status = AppointmentMvcService.parseStatus(dto.status),
            source = AppointmentMvcService.parseSource(dto.source),
            notes = dto.notes,
            createdBy = dto.createdBy
        )
        return crmShellApplicationService.appointmentMapById(id)
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
    }

    @Delete("/{id}")
    @Operation(summary = "Delete appointment")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Not found or cannot delete")
    )
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        appointmentMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
