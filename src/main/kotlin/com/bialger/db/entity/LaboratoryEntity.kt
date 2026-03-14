package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("laboratory")
data class LaboratoryEntity(
    @Id val id: UUID,
    val name: String,
    val integrationType: String? = null,
    val config: String? = null,
    val isActive: Boolean = true
)
