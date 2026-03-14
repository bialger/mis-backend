package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@MappedEntity("inventory_operation")
data class InventoryOperationEntity(
    @Id val id: UUID,
    val itemId: UUID,
    val employeeId: UUID,
    val operationType: String,  // INCOMING, WRITE_OFF_AUTO, WRITE_OFF_MANUAL
    val quantity: BigDecimal,
    val appointmentId: UUID? = null,
    val notes: String? = null,
    val createdAt: Instant? = null
)
