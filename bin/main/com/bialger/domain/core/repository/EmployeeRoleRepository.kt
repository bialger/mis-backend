package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.EmployeeRoleEntity
import jakarta.inject.Singleton
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

interface EmployeeRoleRepository {

    fun findByEmployeeId(employeeId: UUID): List<EmployeeRoleEntity>

    fun findByRoleId(roleId: UUID): List<EmployeeRoleEntity>

    fun save(employeeId: UUID, roleId: UUID)

    fun deleteByEmployeeIdAndRoleId(employeeId: UUID, roleId: UUID)

    fun deleteByEmployeeId(employeeId: UUID)
}

@Singleton
class EmployeeRoleRepositoryImpl(
    private val dataSource: DataSource
) : EmployeeRoleRepository {

    override fun findByEmployeeId(employeeId: UUID): List<EmployeeRoleEntity> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT employee_id, role_id FROM employee_role WHERE employee_id = ?").use { ps ->
                ps.setObject(1, employeeId)
                ps.executeQuery().use { rs -> mapResultSet(rs) }
            }
        }

    override fun findByRoleId(roleId: UUID): List<EmployeeRoleEntity> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT employee_id, role_id FROM employee_role WHERE role_id = ?").use { ps ->
                ps.setObject(1, roleId)
                ps.executeQuery().use { rs -> mapResultSet(rs) }
            }
        }

    override fun save(employeeId: UUID, roleId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("INSERT INTO employee_role (employee_id, role_id) VALUES (?, ?)").use { ps ->
                ps.setObject(1, employeeId)
                ps.setObject(2, roleId)
                ps.executeUpdate()
            }
        }
    }

    override fun deleteByEmployeeIdAndRoleId(employeeId: UUID, roleId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM employee_role WHERE employee_id = ? AND role_id = ?").use { ps ->
                ps.setObject(1, employeeId)
                ps.setObject(2, roleId)
                ps.executeUpdate()
            }
        }
    }

    override fun deleteByEmployeeId(employeeId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM employee_role WHERE employee_id = ?").use { ps ->
                ps.setObject(1, employeeId)
                ps.executeUpdate()
            }
        }
    }

    private fun mapResultSet(rs: ResultSet): List<EmployeeRoleEntity> {
        val result = mutableListOf<EmployeeRoleEntity>()
        while (rs.next()) {
            result.add(
                EmployeeRoleEntity(
                    employeeId = rs.getObject("employee_id", UUID::class.java),
                    roleId = rs.getObject("role_id", UUID::class.java)
                )
            )
        }
        return result
    }
}
