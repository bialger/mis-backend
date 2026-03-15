package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.RolePermissionEntity
import jakarta.inject.Singleton
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

interface RolePermissionRepository {

    fun findByRoleId(roleId: UUID): List<RolePermissionEntity>

    fun findByPermissionId(permissionId: UUID): List<RolePermissionEntity>

    fun save(roleId: UUID, permissionId: UUID)

    fun deleteByRoleIdAndPermissionId(roleId: UUID, permissionId: UUID)
}

@Singleton
class RolePermissionRepositoryImpl(
    private val dataSource: DataSource
) : RolePermissionRepository {

    override fun findByRoleId(roleId: UUID): List<RolePermissionEntity> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT role_id, permission_id FROM role_permission WHERE role_id = ?").use { ps ->
                ps.setObject(1, roleId)
                ps.executeQuery().use { rs -> mapResultSet(rs) }
            }
        }

    override fun findByPermissionId(permissionId: UUID): List<RolePermissionEntity> =
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT role_id, permission_id FROM role_permission WHERE permission_id = ?").use { ps ->
                ps.setObject(1, permissionId)
                ps.executeQuery().use { rs -> mapResultSet(rs) }
            }
        }

    override fun save(roleId: UUID, permissionId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)").use { ps ->
                ps.setObject(1, roleId)
                ps.setObject(2, permissionId)
                ps.executeUpdate()
            }
        }
    }

    override fun deleteByRoleIdAndPermissionId(roleId: UUID, permissionId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?").use { ps ->
                ps.setObject(1, roleId)
                ps.setObject(2, permissionId)
                ps.executeUpdate()
            }
        }
    }

    private fun mapResultSet(rs: ResultSet): List<RolePermissionEntity> {
        val result = mutableListOf<RolePermissionEntity>()
        while (rs.next()) {
            result.add(
                RolePermissionEntity(
                    roleId = rs.getObject("role_id", UUID::class.java),
                    permissionId = rs.getObject("permission_id", UUID::class.java)
                )
            )
        }
        return result
    }
}
