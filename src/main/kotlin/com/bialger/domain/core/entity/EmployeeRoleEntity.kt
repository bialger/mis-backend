package com.bialger.domain.core.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity("employee_role")
data class EmployeeRoleEntity(
    @field:Id
    val employeeId: java.util.UUID,
    @field:Id
    val roleId: java.util.UUID
)
