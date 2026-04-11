package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Appointment response")
data class AppointmentRestDto(
    val id: String,
    val patientId: String,
    val patientName: String?,
    val employeeName: String?,
    val slotLabel: String?,
    val doctorId: String,
    val employeeId: String,
    val branchId: String,
    val roomId: String,
    val status: String,
    val statusApi: String,
    val source: String,
    val start: String?,
    val end: String?,
    val notes: String?,
    val timeSlotId: String?
)

@Serdeable
@Introspected
@Schema(description = "Create appointment")
data class AppointmentCreateDto(
    @field:NotNull @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val patientId: UUID,
    @field:NotNull val employeeId: UUID,
    val timeSlotId: UUID?,
    @field:NotNull val branchId: UUID,
    @field:NotNull val roomId: UUID,
    @field:NotBlank val status: String,
    @field:NotBlank val source: String,
    val notes: String? = null,
    val createdBy: UUID? = null
)

@Serdeable
@Introspected
@Schema(description = "Update appointment")
data class AppointmentUpdateDto(
    @field:NotNull val patientId: UUID,
    @field:NotNull val employeeId: UUID,
    val timeSlotId: UUID?,
    @field:NotNull val branchId: UUID,
    @field:NotNull val roomId: UUID,
    @field:NotBlank val status: String,
    @field:NotBlank val source: String,
    val notes: String? = null,
    val createdBy: UUID? = null
)
