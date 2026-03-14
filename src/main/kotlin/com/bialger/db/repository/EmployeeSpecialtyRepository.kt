package com.bialger.db.repository

import com.bialger.db.entity.EmployeeSpecialtyEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface EmployeeSpecialtyRepository {

    @Query("SELECT employee_id as employeeId, specialty_id as specialtyId FROM employee_specialty WHERE employee_id = :employeeId")
    fun findByEmployeeId(employeeId: UUID): List<EmployeeSpecialtyEntity>

    @Query("SELECT employee_id as employeeId, specialty_id as specialtyId FROM employee_specialty WHERE specialty_id = :specialtyId")
    fun findBySpecialtyId(specialtyId: UUID): List<EmployeeSpecialtyEntity>

    @Query("INSERT INTO employee_specialty (employee_id, specialty_id) VALUES (:employeeId, :specialtyId)")
    fun save(employeeId: UUID, specialtyId: UUID)

    @Query("DELETE FROM employee_specialty WHERE employee_id = :employeeId AND specialty_id = :specialtyId")
    fun deleteByEmployeeIdAndSpecialtyId(employeeId: UUID, specialtyId: UUID)
}
