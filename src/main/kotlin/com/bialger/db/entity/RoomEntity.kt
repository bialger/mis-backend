package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("room")
data class RoomEntity(
    @Id val id: UUID,
    val branchId: UUID,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true
)
