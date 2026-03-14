package com.bialger.db.repository

import com.bialger.db.entity.EmployeeRoleEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface EmployeeRoleRepository {

    @Query("SELECT employee_id, role_id FROM employee_role WHERE employee_id = :employeeId")
    fun findByEmployeeId(employeeId: UUID): List<EmployeeRoleEntity>

    @Query("SELECT employee_id, role_id FROM employee_role WHERE role_id = :roleId")
    fun findByRoleId(roleId: UUID): List<EmployeeRoleEntity>

    @Query("INSERT INTO employee_role (employee_id, role_id) VALUES (:employeeId, :roleId)")
    fun save(employeeId: UUID, roleId: UUID)

    @Query("DELETE FROM employee_role WHERE employee_id = :employeeId AND role_id = :roleId")
    fun deleteByEmployeeIdAndRoleId(employeeId: UUID, roleId: UUID)
}
