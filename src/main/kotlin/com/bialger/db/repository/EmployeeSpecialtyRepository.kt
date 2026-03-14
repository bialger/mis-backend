package com.bialger.db.repository

import com.bialger.db.entity.EmployeeSpecialtyEntity
import jakarta.inject.Singleton
import javax.sql.DataSource
import java.sql.ResultSet
import java.util.UUID

interface EmployeeSpecialtyRepository {

    fun findByEmployeeId(employeeId: UUID): List<EmployeeSpecialtyEntity>

    fun findBySpecialtyId(specialtyId: UUID): List<EmployeeSpecialtyEntity>

    fun save(employeeId: UUID, specialtyId: UUID)

    fun deleteByEmployeeIdAndSpecialtyId(employeeId: UUID, specialtyId: UUID)
}

@Singleton
class EmployeeSpecialtyRepositoryImpl(
    private val dataSource: DataSource
) : EmployeeSpecialtyRepository {

    override fun findByEmployeeId(employeeId: UUID): List<EmployeeSpecialtyEntity> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT employee_id, specialty_id FROM employee_specialty WHERE employee_id = ?").use { ps ->
                ps.setObject(1, employeeId)
                ps.executeQuery().use { rs -> mapResultSet(rs) }
            }
        }

    override fun findBySpecialtyId(specialtyId: UUID): List<EmployeeSpecialtyEntity> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT employee_id, specialty_id FROM employee_specialty WHERE specialty_id = ?").use { ps ->
                ps.setObject(1, specialtyId)
                ps.executeQuery().use { rs -> mapResultSet(rs) }
            }
        }

    override fun save(employeeId: UUID, specialtyId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("INSERT INTO employee_specialty (employee_id, specialty_id) VALUES (?, ?)").use { ps ->
                ps.setObject(1, employeeId)
                ps.setObject(2, specialtyId)
                ps.executeUpdate()
            }
        }
    }

    override fun deleteByEmployeeIdAndSpecialtyId(employeeId: UUID, specialtyId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM employee_specialty WHERE employee_id = ? AND specialty_id = ?").use { ps ->
                ps.setObject(1, employeeId)
                ps.setObject(2, specialtyId)
                ps.executeUpdate()
            }
        }
    }

    private fun mapResultSet(rs: ResultSet): List<EmployeeSpecialtyEntity> {
        val result = mutableListOf<EmployeeSpecialtyEntity>()
        while (rs.next()) {
            result.add(
                EmployeeSpecialtyEntity(
                    employeeId = rs.getObject("employee_id", UUID::class.java),
                    specialtyId = rs.getObject("specialty_id", UUID::class.java)
                )
            )
        }
        return result
    }
}
