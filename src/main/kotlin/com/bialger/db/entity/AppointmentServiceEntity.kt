package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity("appointment_service")
data class AppointmentServiceEntity(
    @Id val id: UUID,
    val appointmentId: UUID,
    val serviceId: UUID,
    val quantity: Int = 1,
    val price: BigDecimal? = null
)
