package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("role_permission")
data class RolePermissionEntity(
    @field:Id val roleId: UUID,
    @field:Id val permissionId: UUID
)
