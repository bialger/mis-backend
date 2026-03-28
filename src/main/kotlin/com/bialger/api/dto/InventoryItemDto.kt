package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Создание позиции склада")
data class InventoryItemCreateDto(
    @field:NotNull val categoryId: UUID,
    @field:NotNull val branchId: UUID,
    val roomId: UUID?,
    @field:NotBlank val name: String,
    val unit: String?,
    @field:NotNull val quantity: BigDecimal,
    val minQuantity: BigDecimal?,
    val costPrice: BigDecimal?
)

@Serdeable
@Introspected
@Schema(description = "Обновление позиции склада")
data class InventoryItemUpdateDto(
    @field:NotNull val categoryId: UUID,
    @field:NotNull val branchId: UUID,
    val roomId: UUID?,
    @field:NotBlank val name: String,
    val unit: String?,
    @field:NotNull val quantity: BigDecimal,
    val minQuantity: BigDecimal?,
    val costPrice: BigDecimal?
)
