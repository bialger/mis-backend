package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Создание записи")
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
@Schema(description = "Обновление записи")
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
