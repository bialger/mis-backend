package com.bialger.db.repository

import com.bialger.db.entity.EmployeePermissionEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface EmployeePermissionRepository : CrudRepository<EmployeePermissionEntity, UUID> {

    fun findByEmployeeId(employeeId: UUID): List<EmployeePermissionEntity>

    fun findByPermissionId(permissionId: UUID): List<EmployeePermissionEntity>
}
