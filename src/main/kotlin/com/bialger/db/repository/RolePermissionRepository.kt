package com.bialger.db.repository

import com.bialger.db.entity.RolePermissionEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface RolePermissionRepository {

    @Query("SELECT role_id, permission_id FROM role_permission WHERE role_id = :roleId")
    fun findByRoleId(roleId: UUID): List<RolePermissionEntity>

    @Query("SELECT role_id, permission_id FROM role_permission WHERE permission_id = :permissionId")
    fun findByPermissionId(permissionId: UUID): List<RolePermissionEntity>

    @Query("INSERT INTO role_permission (role_id, permission_id) VALUES (:roleId, :permissionId)")
    fun save(roleId: UUID, permissionId: UUID)

    @Query("DELETE FROM role_permission WHERE role_id = :roleId AND permission_id = :permissionId")
    fun deleteByRoleIdAndPermissionId(roleId: UUID, permissionId: UUID)
}
