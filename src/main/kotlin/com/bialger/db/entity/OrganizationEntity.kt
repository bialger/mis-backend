package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.util.UUID

@MappedEntity("organization")
data class OrganizationEntity(
    @Id val id: UUID,
    val name: String,
    val codeOkpo: String? = null,
    val codeOkud: String? = null,
    val address: String? = null,
    val createdAt: Instant? = null
)
