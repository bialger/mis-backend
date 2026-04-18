package com.bialger.api

import com.bialger.api.dto.PublicBookingBranchDto
import com.bialger.api.dto.PublicBookingCreateAppointmentDto
import com.bialger.api.dto.PublicBookingCreatedDto
import com.bialger.api.dto.PublicBookingDoctorDto
import com.bialger.api.dto.PublicBookingSlotDto
import com.bialger.application.booking.PublicBookingService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.LocalDate
import java.util.UUID

@Controller("/api/public/booking")
@Tag(name = "PublicBooking", description = "Anonymous online patient booking")
open class PublicBookingApiController(
    private val publicBookingService: PublicBookingService
) {

    @Get("/branches", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List active branches for public online booking")
    open fun branches(): List<PublicBookingBranchDto> = publicBookingService.listBranches()

    @Get("/doctors", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List online-booking doctors for a branch")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Doctors list"),
        ApiResponse(responseCode = "400", description = "Invalid or unknown branchId")
    )
    open fun doctors(@QueryValue branchId: UUID): List<PublicBookingDoctorDto> =
        publicBookingService.listDoctors(branchId)

    @Get("/slots", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List one-day time slots for selected branch and doctor")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Slots list"),
        ApiResponse(responseCode = "400", description = "Invalid branch/doctor/date")
    )
    open fun slots(
        @QueryValue branchId: UUID,
        @QueryValue employeeId: UUID,
        @QueryValue slotDate: String
    ): List<PublicBookingSlotDto> {
        val date = runCatching { LocalDate.parse(slotDate.trim()) }
            .getOrElse { throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid slotDate") }
        return publicBookingService.listSlots(
            branchId = branchId,
            employeeId = employeeId,
            slotDate = date
        )
    }

    @Post("/appointments", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create patient and appointment for public booking")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created appointment"),
        ApiResponse(responseCode = "400", description = "Validation or mismatch error"),
        ApiResponse(responseCode = "409", description = "Slot already occupied")
    )
    open fun createAppointment(@Body @Valid dto: PublicBookingCreateAppointmentDto): PublicBookingCreatedDto =
        publicBookingService.createAppointment(dto)
}
