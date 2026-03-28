package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@Introspected
data class PaymentPatchDto(
    val appointmentId: String? = null,
    val amount: String? = null,
    val paymentMethod: String? = null,
    val paymentStatus: String? = null,
    val notes: String? = null,
    val createdBy: String? = null
)
