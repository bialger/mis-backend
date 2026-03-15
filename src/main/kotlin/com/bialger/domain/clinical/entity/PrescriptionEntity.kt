package com.bialger.domain.clinical.entity

import com.bialger.db.converter.PrescriptionTypeConverter
import com.bialger.domain.clinical.enums.PrescriptionType
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("prescription")
data class PrescriptionEntity(
    @Id val id: UUID,
    val medicalRecordId: UUID,
    @field:TypeDef(type = DataType.OBJECT, converter = PrescriptionTypeConverter::class)
    val type: PrescriptionType,
    val description: String,
    val createdAt: Instant? = null
)
