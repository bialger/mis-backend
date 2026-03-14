package com.bialger.db.entity

import com.bialger.db.converter.IntegrationTypeConverter
import com.bialger.db.converter.JsonbConverter
import com.bialger.db.enums.IntegrationType
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("integration")
data class IntegrationEntity(
    @Id val id: UUID,
    @field:TypeDef(type = DataType.OBJECT, converter = IntegrationTypeConverter::class)
    val type: IntegrationType,
    val name: String,
    @field:TypeDef(type = DataType.OBJECT, converter = JsonbConverter::class)
    val config: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant? = null
)
