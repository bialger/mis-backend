package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@MappedEntity("payment")
data class PaymentEntity(
    @Id val id: UUID,
    val appointmentId: UUID,
    val amount: BigDecimal,
    val paymentMethod: String,      // CASH, CARD, TRANSFER, OTHER
    val paymentStatus: String,     // PAID, PARTIAL, DEFERRED, NO_CASH_REGISTER
    val notes: String? = null,
    val createdBy: UUID,
    val createdAt: Instant? = null
)
