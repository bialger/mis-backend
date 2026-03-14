package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("template")
data class TemplateEntity(
    @Id val id: UUID,
    val name: String,
    val type: String,  // STANDARD, CLINICAL_GUIDELINE, PERSONAL
    val specialtyId: UUID? = null,
    val employeeId: UUID? = null,
    val content: String,
    val isActive: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
