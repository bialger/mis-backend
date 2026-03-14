package com.bialger.db.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity("employee_specialty")
data class EmployeeSpecialtyEntity(
    @field:Id
    val employeeId: java.util.UUID,
    @field:Id
    val specialtyId: java.util.UUID
)
