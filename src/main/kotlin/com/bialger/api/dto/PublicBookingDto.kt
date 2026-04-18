package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Public branch item for online booking")
data class PublicBookingBranchDto(
    val id: String,
    val organizationId: String,
    val name: String,
    val startTime: String,
    val endTime: String
)

@Serdeable
@Introspected
@Schema(description = "Public doctor item for online booking")
data class PublicBookingDoctorDto(
    val id: String,
    val name: String,
    val workStartTime: String?,
    val workEndTime: String?
)

@Serdeable
@Introspected
@Schema(description = "Public time slot item for online booking")
data class PublicBookingSlotDto(
    val id: String,
    val timeSlotId: String?,
    val branchId: String,
    val employeeId: String,
    val roomId: String,
    val slotDate: String,
    val startTime: String,
    val endTime: String,
    val start: String,
    val end: String,
    val roomName: String,
    val free: Boolean
)

@Serdeable
@Introspected
@Schema(description = "Create online booking request")
data class PublicBookingCreateAppointmentDto(
    @field:NotNull val branchId: UUID,
    @field:NotNull val employeeId: UUID,
    val timeSlotId: UUID? = null,
    val slotDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    @field:NotBlank val fullName: String,
    @field:NotBlank val phone: String,
    val comment: String? = null
)

@Serdeable
@Introspected
@Schema(description = "Create online booking response")
data class PublicBookingCreatedDto(
    val patientId: String,
    val appointmentId: String,
    val status: String,
    val source: String,
    val message: String
)
