package com.bialger.db.entity

import com.bialger.db.converter.PaymentMethodTypeConverter
import com.bialger.db.converter.PaymentStatusTypeConverter
import com.bialger.db.enums.PaymentMethodType
import com.bialger.db.enums.PaymentStatusType
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@MappedEntity("payment")
data class PaymentEntity(
    @Id val id: UUID,
    val appointmentId: UUID,
    val amount: BigDecimal,
    @field:TypeDef(type = DataType.OBJECT, converter = PaymentMethodTypeConverter::class)
    val paymentMethod: PaymentMethodType,
    @field:TypeDef(type = DataType.OBJECT, converter = PaymentStatusTypeConverter::class)
    val paymentStatus: PaymentStatusType,
    val notes: String? = null,
    val createdBy: UUID,
    val createdAt: Instant? = null
)
