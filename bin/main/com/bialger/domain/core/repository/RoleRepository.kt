package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.RoleEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface RoleRepository : CrudRepository<RoleEntity, UUID> {

    @Query("SELECT * FROM role ORDER BY name")
    fun findAllOrdered(): List<RoleEntity>

    @Query("SELECT * FROM role WHERE id IN (:ids)")
    fun findByIds(ids: List<UUID>): List<RoleEntity>

    fun findByName(name: String): RoleEntity?
}
