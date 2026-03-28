package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Создание системной настройки")
data class SystemSettingCreateDto(
    val branchId: UUID?,
    @field:NotBlank val key: String,
    val value: String?,
    val description: String?
)

@Serdeable
@Introspected
@Schema(description = "Обновление системной настройки")
data class SystemSettingUpdateDto(
    val branchId: UUID?,
    @field:NotBlank val key: String,
    val value: String?,
    val description: String?
)
