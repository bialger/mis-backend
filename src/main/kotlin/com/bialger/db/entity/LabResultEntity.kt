package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("lab_result")
data class LabResultEntity(
    @Id val id: UUID,
    val labOrderItemId: UUID,
    val resultData: String? = null,
    val source: String,
    val receivedAt: Instant? = null,
    val isSentToGov: Boolean = false,
    val sentToGovAt: Instant? = null,
    val sentToGovBy: UUID? = null
)
