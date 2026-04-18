package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Serdeable
@Introspected
@Schema(description = "Organization")
data class OrganizationRestDto(
    val id: String,
    val name: String,
    val codeOkpo: String?,
    val codeOkud: String?,
    val address: String?
)

@Serdeable
@Introspected
@Schema(description = "Create organization")
data class OrganizationCreateDto(
    @field:NotBlank val name: String,
    val codeOkpo: String? = null,
    val codeOkud: String? = null,
    val address: String? = null
)

@Serdeable
@Introspected
@Schema(description = "Update organization")
data class OrganizationUpdateDto(
    @field:NotBlank val name: String,
    val codeOkpo: String? = null,
    val codeOkud: String? = null,
    val address: String? = null
)
