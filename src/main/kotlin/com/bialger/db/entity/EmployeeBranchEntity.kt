package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity("employee_branch")
data class EmployeeBranchEntity(
    @field:Id
    val employeeId: java.util.UUID,
    @field:Id
    val branchId: java.util.UUID
)
