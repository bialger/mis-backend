package com.bialger.domain.scheduling.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@MappedEntity("time_slot")
data class TimeSlotEntity(
    @Id val id: UUID,
    val employeeId: UUID,
    val roomId: UUID,
    val branchId: UUID,
    val slotDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isAvailable: Boolean = true
)
