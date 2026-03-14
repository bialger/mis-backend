package com.bialger.db.entity

import com.bialger.db.converter.TemplateTypeConverter
import com.bialger.db.enums.TemplateType
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("template")
data class TemplateEntity(
    @Id val id: UUID,
    val name: String,
    @field:TypeDef(type = DataType.OBJECT, converter = TemplateTypeConverter::class)
    val type: TemplateType,
    val specialtyId: UUID? = null,
    val employeeId: UUID? = null,
    val content: String,
    val isActive: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
