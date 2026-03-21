package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.EmployeeBranchEntity
import jakarta.inject.Singleton
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

interface EmployeeBranchRepository {

    fun findByEmployeeId(employeeId: UUID): List<EmployeeBranchEntity>

    fun findByBranchId(branchId: UUID): List<EmployeeBranchEntity>

    fun save(employeeId: UUID, branchId: UUID)

    fun deleteByEmployeeIdAndBranchId(employeeId: UUID, branchId: UUID)

    fun deleteByEmployeeId(employeeId: UUID)
}

@Singleton
class EmployeeBranchRepositoryImpl(
    private val dataSource: DataSource
) : EmployeeBranchRepository {

    override fun findByEmployeeId(employeeId: UUID): List<EmployeeBranchEntity> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT employee_id, branch_id FROM employee_branch WHERE employee_id = ?").use { ps ->
                ps.setObject(1, employeeId)
                ps.executeQuery().use { rs -> mapResultSet(rs) }
            }
        }

    override fun findByBranchId(branchId: UUID): List<EmployeeBranchEntity> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT employee_id, branch_id FROM employee_branch WHERE branch_id = ?").use { ps ->
                ps.setObject(1, branchId)
                ps.executeQuery().use { rs -> mapResultSet(rs) }
            }
        }

    override fun save(employeeId: UUID, branchId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("INSERT INTO employee_branch (employee_id, branch_id) VALUES (?, ?)").use { ps ->
                ps.setObject(1, employeeId)
                ps.setObject(2, branchId)
                ps.executeUpdate()
            }
        }
    }

    override fun deleteByEmployeeIdAndBranchId(employeeId: UUID, branchId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM employee_branch WHERE employee_id = ? AND branch_id = ?").use { ps ->
                ps.setObject(1, employeeId)
                ps.setObject(2, branchId)
                ps.executeUpdate()
            }
        }
    }

    override fun deleteByEmployeeId(employeeId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM employee_branch WHERE employee_id = ?").use { ps ->
                ps.setObject(1, employeeId)
                ps.executeUpdate()
            }
        }
    }

    private fun mapResultSet(rs: ResultSet): List<EmployeeBranchEntity> {
        val result = mutableListOf<EmployeeBranchEntity>()
        while (rs.next()) {
            result.add(
                EmployeeBranchEntity(
                    employeeId = rs.getObject("employee_id", UUID::class.java),
                    branchId = rs.getObject("branch_id", UUID::class.java)
                )
            )
        }
        return result
    }
}
