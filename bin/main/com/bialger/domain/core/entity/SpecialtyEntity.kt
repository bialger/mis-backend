package com.bialger.domain.core.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("specialty")
data class SpecialtyEntity(
    @Id val id: UUID,
    val name: String,
    val description: String? = null
)
