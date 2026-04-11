package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "System setting response")
data class SystemSettingRestDto(
    val id: String,
    val branchId: String,
    val key: String,
    val value: String,
    val description: String
)

@Serdeable
@Introspected
@Schema(description = "Create system setting")
data class SystemSettingCreateDto(
    val branchId: UUID?,
    @field:NotBlank val key: String,
    val value: String?,
    val description: String?
)

@Serdeable
@Introspected
@Schema(description = "Update system setting")
data class SystemSettingUpdateDto(
    val branchId: UUID?,
    @field:NotBlank val key: String,
    val value: String?,
    val description: String?
)
