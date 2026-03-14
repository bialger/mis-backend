package com.bialger.db.entity

import com.bialger.db.converter.JsonbConverter
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.util.UUID

@MappedEntity("laboratory")
data class LaboratoryEntity(
    @Id val id: UUID,
    val name: String,
    val integrationType: String? = null,
    @field:TypeDef(type = DataType.OBJECT, converter = JsonbConverter::class)
    val config: String? = null,
    val isActive: Boolean = true
)
