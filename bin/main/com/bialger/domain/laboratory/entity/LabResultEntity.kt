package com.bialger.domain.laboratory.entity

import com.bialger.db.converter.LabResultSourceConverter
import com.bialger.domain.laboratory.enums.LabResultSource
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("lab_result")
data class LabResultEntity(
    @Id val id: UUID,
    val labOrderItemId: UUID,
    val resultData: String? = null,
    @field:TypeDef(type = DataType.OBJECT, converter = LabResultSourceConverter::class)
    val source: LabResultSource,
    val receivedAt: Instant? = null,
    val isSentToGov: Boolean = false,
    val sentToGovAt: Instant? = null,
    val sentToGovBy: UUID? = null
)
