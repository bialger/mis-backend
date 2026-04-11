package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Introspected
@Schema(description = "Payment response")
data class PaymentRestDto(
    val id: String,
    val appointmentId: String,
    val amount: String,
    val paidAmount: String?,
    val total: Double,
    val paid: Double,
    val paymentMethod: String,
    val paymentStatus: String,
    val method: String,
    val notes: String,
    val createdBy: String
)

@Serdeable
@Introspected
data class PaymentPatchDto(
    val appointmentId: String? = null,
    val amount: String? = null,
    val paidAmount: String? = null,
    val paymentMethod: String? = null,
    val paymentStatus: String? = null,
    val notes: String? = null,
    val createdBy: String? = null
)
