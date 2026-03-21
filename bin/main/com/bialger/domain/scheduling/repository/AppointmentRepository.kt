package com.bialger.domain.scheduling.repository

import com.bialger.domain.scheduling.entity.AppointmentEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AppointmentRepository : CrudRepository<AppointmentEntity, UUID> {

    @Query("SELECT * FROM appointment ORDER BY created_at DESC NULLS LAST, id")
    fun findAllOrdered(): List<AppointmentEntity>

    @Query("SELECT * FROM appointment ORDER BY created_at DESC NULLS LAST, id LIMIT :limit")
    fun findTopOrdered(limit: Int): List<AppointmentEntity>

    @Query("SELECT * FROM appointment WHERE id IN (:ids)")
    fun findByIds(ids: List<UUID>): List<AppointmentEntity>

    fun findByPatientId(patientId: UUID): List<AppointmentEntity>

    fun findByEmployeeId(employeeId: UUID): List<AppointmentEntity>

    fun findByBranchId(branchId: UUID): List<AppointmentEntity>
}
