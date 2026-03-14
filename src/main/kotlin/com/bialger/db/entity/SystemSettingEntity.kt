package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("system_setting")
data class SystemSettingEntity(
    @Id val id: UUID,
    val branchId: UUID? = null,
    val key: String,
    val value: String? = null,
    val description: String? = null
)
