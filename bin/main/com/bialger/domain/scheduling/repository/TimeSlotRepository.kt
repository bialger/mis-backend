package com.bialger.domain.scheduling.repository

import com.bialger.domain.scheduling.entity.TimeSlotEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.LocalDate
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface TimeSlotRepository : CrudRepository<TimeSlotEntity, UUID> {

    @Query("SELECT * FROM time_slot ORDER BY slot_date DESC, start_time")
    fun findAllOrdered(): List<TimeSlotEntity>

    @Query("SELECT * FROM time_slot ORDER BY slot_date DESC, start_time DESC LIMIT :limit")
    fun findTopOrdered(limit: Int): List<TimeSlotEntity>

    @Query("SELECT * FROM time_slot WHERE id IN (:ids)")
    fun findByIds(ids: List<UUID>): List<TimeSlotEntity>

    fun findByEmployeeId(employeeId: UUID): List<TimeSlotEntity>

    fun findByBranchId(branchId: UUID): List<TimeSlotEntity>

    fun findByBranchIdAndSlotDate(branchId: UUID, slotDate: LocalDate): List<TimeSlotEntity>
}
