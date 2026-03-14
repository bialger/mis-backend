package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("integration")
data class IntegrationEntity(
    @Id val id: UUID,
    val type: String,     // SMS_PROVIDER, LABORATORY, GOV_SYSTEM
    val name: String,
    val config: String? = null,  // JSONB as String
    val isActive: Boolean = true,
    val createdAt: Instant? = null
)
