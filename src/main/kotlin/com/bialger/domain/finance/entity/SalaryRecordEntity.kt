package com.bialger.domain.finance.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@MappedEntity("salary_record")
data class SalaryRecordEntity(
    @Id val id: UUID,
    val employeeId: UUID,
    val branchId: UUID,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val hoursWorked: BigDecimal? = null,
    val shiftsCount: Int? = null,
    val amount: BigDecimal? = null,
    val createdAt: Instant? = null
)
