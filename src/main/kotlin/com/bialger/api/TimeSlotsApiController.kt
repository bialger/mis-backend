package com.bialger.api

import com.bialger.api.dto.TimeSlotCreateDto
import com.bialger.api.dto.TimeSlotUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.application.shell.CrmShellApplicationService
import com.bialger.domain.scheduling.mvc.TimeSlotMvcService
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

@Controller("/api/time-slots")
@Tag(name = "TimeSlots", description = "Schedule time slots (online booking)")
open class TimeSlotsApiController(
    private val timeSlotMvcService: TimeSlotMvcService,
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List time slots (paginated)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when adjacent pages exist"),
        ApiResponse(responseCode = "400", description = "Invalid page/size parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<Map<String, Any?>>> {
        val rows = crmShellApplicationService.timeSlotsList()
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get time slot by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Time slot"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): Map<String, Any?> {
        val row = timeSlotMvcService.listRows().find { it.slot.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return crmShellApplicationService.timeSlotToMap(row)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create time slot")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created"),
        ApiResponse(responseCode = "400", description = "Validation error")
    )
    open fun create(@Body @Valid dto: TimeSlotCreateDto): Map<String, Any?> {
        val slotDate = TimeSlotMvcService.parseLocalDate(dto.slotDate)
        val startTime = TimeSlotMvcService.parseLocalTime(dto.startTime)
        val endTime = TimeSlotMvcService.parseLocalTime(dto.endTime)
        val e = timeSlotMvcService.create(
            employeeId = dto.employeeId,
            roomId = dto.roomId,
            branchId = dto.branchId,
            slotDate = slotDate,
            startTime = startTime,
            endTime = endTime,
            isAvailable = dto.isAvailable
        )
        val row = timeSlotMvcService.listRows().find { it.slot.id == e.id }
            ?: throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load time slot")
        return crmShellApplicationService.timeSlotToMap(row)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update time slot")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: TimeSlotUpdateDto): Map<String, Any?> {
        val slotDate = TimeSlotMvcService.parseLocalDate(dto.slotDate)
        val startTime = TimeSlotMvcService.parseLocalTime(dto.startTime)
        val endTime = TimeSlotMvcService.parseLocalTime(dto.endTime)
        timeSlotMvcService.update(
            id = id,
            employeeId = dto.employeeId,
            roomId = dto.roomId,
            branchId = dto.branchId,
            slotDate = slotDate,
            startTime = startTime,
            endTime = endTime,
            isAvailable = dto.isAvailable
        )
        val row = timeSlotMvcService.listRows().find { it.slot.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return crmShellApplicationService.timeSlotToMap(row)
    }

    @Delete("/{id}")
    @Operation(summary = "Delete time slot")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Not found or cannot delete")
    )
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        timeSlotMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
