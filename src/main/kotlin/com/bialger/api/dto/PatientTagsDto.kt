package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Serdeable
@Introspected
@Schema(description = "Patient tag type ids response")
data class PatientTagsResponseDto(
    val tagTypeIds: List<String>
)

@Serdeable
@Introspected
@Schema(description = "Replace patient tags request")
data class PatientTagsReplaceDto(
    val tagTypeIds: List<UUID> = emptyList()
)
