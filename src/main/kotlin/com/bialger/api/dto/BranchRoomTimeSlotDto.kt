package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Branch response")
data class BranchRestDto(
    val id: String,
    val organizationId: String,
    val organizationName: String?,
    val name: String,
    val address: String?,
    val phone: String?,
    val isActive: Boolean,
    val startTime: String,
    val endTime: String
)

@Serdeable
@Introspected
@Schema(description = "Room response")
data class RoomRestDto(
    val id: String,
    val branchId: String,
    val name: String,
    val description: String?,
    val isActive: Boolean
)

@Serdeable
@Introspected
@Schema(description = "Time slot response")
data class TimeSlotRestDto(
    val id: String,
    val employeeId: String,
    val branchId: String,
    val roomId: String,
    val slotDate: String,
    val start: String?,
    val end: String?,
    val startTime: String,
    val endTime: String,
    val employeeName: String?,
    val roomName: String?,
    val branchName: String?,
    val isAvailable: Boolean
)

@Serdeable
@Introspected
@Schema(description = "Create branch")
data class BranchCreateDto(
    @field:NotNull @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    val organizationId: UUID,
    @field:NotBlank val name: String,
    val address: String? = null,
    val phone: String? = null,
    val isActive: Boolean = true,
    val startTime: String? = null,
    val endTime: String? = null
)

@Serdeable
@Introspected
@Schema(description = "Update branch")
data class BranchUpdateDto(
    @field:NotNull val organizationId: UUID,
    @field:NotBlank val name: String,
    val address: String? = null,
    val phone: String? = null,
    val isActive: Boolean = true,
    val startTime: String? = null,
    val endTime: String? = null
)

@Serdeable
@Introspected
@Schema(description = "Create room")
data class RoomCreateDto(
    @field:NotNull val branchId: UUID,
    @field:NotBlank val name: String,
    val description: String? = null,
    val isActive: Boolean = true
)

@Serdeable
@Introspected
@Schema(description = "Update room")
data class RoomUpdateDto(
    @field:NotNull val branchId: UUID,
    @field:NotBlank val name: String,
    val description: String? = null,
    val isActive: Boolean = true
)

@Serdeable
@Introspected
@Schema(description = "Create time slot")
data class TimeSlotCreateDto(
    @field:NotNull val employeeId: UUID,
    @field:NotNull val roomId: UUID,
    @field:NotNull val branchId: UUID,
    @field:NotBlank val slotDate: String,
    @field:NotBlank val startTime: String,
    @field:NotBlank val endTime: String,
    val isAvailable: Boolean = true
)

@Serdeable
@Introspected
@Schema(description = "Update time slot")
data class TimeSlotUpdateDto(
    @field:NotNull val employeeId: UUID,
    @field:NotNull val roomId: UUID,
    @field:NotNull val branchId: UUID,
    @field:NotBlank val slotDate: String,
    @field:NotBlank val startTime: String,
    @field:NotBlank val endTime: String,
    val isAvailable: Boolean = true
)
