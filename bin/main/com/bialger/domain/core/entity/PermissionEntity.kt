package com.bialger.domain.core.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("permission")
data class PermissionEntity(
    @Id val id: UUID,
    val code: String,
    val name: String? = null,
    val description: String? = null
)
