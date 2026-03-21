package com.bialger.domain.laboratory.entity

import com.bialger.db.converter.LabOrderStatusConverter
import com.bialger.domain.laboratory.enums.LabOrderStatus
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@MappedEntity("lab_order")
data class LabOrderEntity(
    @Id val id: UUID,
    val medicalRecordId: UUID,
    val patientId: UUID,
    val employeeId: UUID,
    val totalPrice: BigDecimal? = null,
    @field:TypeDef(type = DataType.OBJECT, converter = LabOrderStatusConverter::class)
    val status: LabOrderStatus,
    val createdAt: Instant? = null
)
