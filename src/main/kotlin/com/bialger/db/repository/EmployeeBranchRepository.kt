package com.bialger.db.repository

import com.bialger.db.entity.EmployeeBranchEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface EmployeeBranchRepository {

    @Query("SELECT employee_id, branch_id FROM employee_branch WHERE employee_id = :employeeId")
    fun findByEmployeeId(employeeId: UUID): List<EmployeeBranchEntity>

    @Query("SELECT employee_id, branch_id FROM employee_branch WHERE branch_id = :branchId")
    fun findByBranchId(branchId: UUID): List<EmployeeBranchEntity>

    @Query("INSERT INTO employee_branch (employee_id, branch_id) VALUES (:employeeId, :branchId)")
    fun save(employeeId: UUID, branchId: UUID)

    @Query("DELETE FROM employee_branch WHERE employee_id = :employeeId AND branch_id = :branchId")
    fun deleteByEmployeeIdAndBranchId(employeeId: UUID, branchId: UUID)
}
