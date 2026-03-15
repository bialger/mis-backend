package com.bialger.domain.scheduling.repository

import com.bialger.domain.scheduling.entity.TimeSlotEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.LocalDate
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface TimeSlotRepository : CrudRepository<TimeSlotEntity, UUID> {

    fun findByEmployeeId(employeeId: UUID): List<TimeSlotEntity>

    fun findByBranchId(branchId: UUID): List<TimeSlotEntity>

    fun findByBranchIdAndSlotDate(branchId: UUID, slotDate: LocalDate): List<TimeSlotEntity>
}
