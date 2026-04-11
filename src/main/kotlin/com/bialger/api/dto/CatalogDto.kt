package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Introspected
@Schema(description = "Medical service (price list entry)")
data class CatalogServiceRestDto(
    val id: String,
    val name: String,
    val price: String,
    val costPrice: String?,
    val branchId: String?,
    val isActive: Boolean
)

@Serdeable
@Introspected
@Schema(description = "Document template")
data class CatalogTemplateRestDto(
    val id: String,
    val name: String,
    val type: String,
    val specialtyId: String?,
    val employeeId: String?,
    val isActive: Boolean,
    val contentPreview: String
)

@Serdeable
@Introspected
@Schema(description = "External integration")
data class CatalogIntegrationRestDto(
    val id: String,
    val name: String,
    val type: String,
    val isActive: Boolean,
    val config: String
)

@Serdeable
@Introspected
@Schema(description = "Patient tag / icon type")
data class PatientTagTypeRestDto(
    val id: String,
    val code: String,
    val name: String,
    val icon: String,
    val description: String,
    val isActive: Boolean
)
