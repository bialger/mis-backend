package com.bialger.domain.system.repository

import com.bialger.domain.system.entity.SystemSettingEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SystemSettingRepository : CrudRepository<SystemSettingEntity, UUID> {

    @Query("SELECT * FROM system_setting ORDER BY key")
    fun findAllOrdered(): List<SystemSettingEntity>

    fun findByBranchId(branchId: UUID?): List<SystemSettingEntity>

    fun findByBranchIdIsNull(): List<SystemSettingEntity>

    fun findByBranchIdAndKey(branchId: UUID?, key: String): SystemSettingEntity?
}
