package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity("employee_permission")
data class EmployeePermissionEntity(
    @Id val id: UUID,
    val employeeId: UUID,
    val permissionId: UUID,
    val isGranted: Boolean
)
