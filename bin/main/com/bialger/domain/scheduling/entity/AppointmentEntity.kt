package com.bialger.domain.scheduling.entity

import com.bialger.db.converter.AppointmentSourceConverter
import com.bialger.db.converter.AppointmentStatusConverter
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("appointment")
data class AppointmentEntity(
    @Id val id: UUID,
    val patientId: UUID,
    val employeeId: UUID,
    val timeSlotId: UUID? = null,
    val branchId: UUID,
    val roomId: UUID,
    @field:TypeDef(type = DataType.OBJECT, converter = AppointmentStatusConverter::class)
    val status: AppointmentStatus,
    @field:TypeDef(type = DataType.OBJECT, converter = AppointmentSourceConverter::class)
    val source: AppointmentSource,
    val notes: String? = null,
    val createdBy: UUID? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
