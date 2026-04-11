package com.bialger.api

import com.bialger.api.dto.AppointmentCreateDto
import com.bialger.api.dto.AppointmentRestDto
import com.bialger.api.dto.AppointmentUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.mvc.AppointmentListRow
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import com.bialger.domain.scheduling.repository.TimeSlotRepository
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
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Controller("/api/appointments")
@Tag(name = "Appointments", description = "Appointments (schedule and visits)")
open class AppointmentsApiController(
    private val appointmentMvcService: AppointmentMvcService,
    private val timeSlotRepository: TimeSlotRepository
) {

    private fun slotsMap(): Map<UUID, TimeSlotEntity> =
        timeSlotRepository.findAllOrdered().associateBy { it.id }

    private fun rowToDto(row: AppointmentListRow, slots: Map<UUID, TimeSlotEntity>): AppointmentRestDto {
        val a = row.appointment
        val start = resolveStart(a, slots)
        val end = resolveEnd(a, slots)
        return AppointmentRestDto(
            id = a.id.toString(),
            patientId = a.patientId.toString(),
            patientName = row.patientName,
            employeeName = row.employeeName,
            slotLabel = row.slotLabel,
            doctorId = a.employeeId.toString(),
            employeeId = a.employeeId.toString(),
            branchId = a.branchId.toString(),
            roomId = a.roomId.toString(),
            status = mapStatusToFrontend(a.status),
            statusApi = a.status.name,
            source = a.source.name,
            start = start?.toString(),
            end = end?.toString(),
            notes = a.notes,
            timeSlotId = a.timeSlotId?.toString()
        )
    }

    private fun resolveStart(a: AppointmentEntity, slots: Map<UUID, TimeSlotEntity>): Instant? {
        val t = a.timeSlotId?.let { slots[it] } ?: return a.createdAt
        return t.slotDate.atTime(t.startTime).atZone(ZoneId.systemDefault()).toInstant()
    }

    private fun resolveEnd(a: AppointmentEntity, slots: Map<UUID, TimeSlotEntity>): Instant? {
        val t = a.timeSlotId?.let { slots[it] }
        return if (t != null) {
            t.slotDate.atTime(t.endTime).atZone(ZoneId.systemDefault()).toInstant()
        } else {
            val s = a.createdAt ?: return null
            Instant.ofEpochMilli(s.toEpochMilli() + 30 * 60_000L)
        }
    }

    private fun mapStatusToFrontend(s: AppointmentStatus): String = when (s) {
        AppointmentStatus.SCHEDULED -> "BOOKED"
        AppointmentStatus.CONFIRMED -> "CONFIRMED"
        AppointmentStatus.ARRIVED -> "CONFIRMED"
        AppointmentStatus.NO_SHOW -> "CANCELLED"
        AppointmentStatus.CANCELLED -> "CANCELLED"
    }

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List appointments")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of appointments; Link header when multiple pages exist",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<AppointmentRestDto>> {
        val slots = slotsMap()
        val rows = appointmentMvcService.listRows().map { rowToDto(it, slots) }
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get appointment by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Appointment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): AppointmentRestDto {
        val slots = slotsMap()
        val row = appointmentMvcService.listRows().find { it.appointment.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return rowToDto(row, slots)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create appointment")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created appointment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation or business rule error"),
        ApiResponse(responseCode = "500", description = "Could not build response after create")
    )
    open fun create(@Body @Valid dto: AppointmentCreateDto): AppointmentRestDto {
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
        val slots = slotsMap()
        val row = appointmentMvcService.listRows().find { it.appointment.id == e.id }
            ?: throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load appointment")
        return rowToDto(row, slots)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update appointment")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated appointment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation or business error"),
        ApiResponse(responseCode = "404", description = "Appointment not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: AppointmentUpdateDto): AppointmentRestDto {
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
        val slots = slotsMap()
        val row = appointmentMvcService.listRows().find { it.appointment.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return rowToDto(row, slots)
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
