package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.PermissionEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PermissionRepository : CrudRepository<PermissionEntity, UUID> {

    @Query("SELECT * FROM permission ORDER BY code")
    fun findAllOrdered(): List<PermissionEntity>

    fun findByCode(code: String): PermissionEntity?
}
