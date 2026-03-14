package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("branch")
data class BranchEntity(
    @Id val id: UUID,
    val organizationId: UUID,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant? = null
)
