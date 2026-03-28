package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class PatientTagsReplaceDto(
    val tagTypeIds: List<UUID> = emptyList()
)
