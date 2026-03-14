package com.bialger.db.repository

import com.bialger.db.entity.PermissionEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PermissionRepository : CrudRepository<PermissionEntity, UUID> {

    fun findByCode(code: String): PermissionEntity?
}
