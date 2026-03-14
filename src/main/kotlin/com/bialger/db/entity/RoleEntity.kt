package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("role")
data class RoleEntity(
    @Id val id: UUID,
    val name: String,
    val displayName: String? = null,
    val description: String? = null
)
