package com.bialger.db.repository

import com.bialger.db.entity.SystemSettingEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SystemSettingRepository : CrudRepository<SystemSettingEntity, UUID> {

    fun findByBranchId(branchId: UUID?): List<SystemSettingEntity>

    fun findByBranchIdIsNull(): List<SystemSettingEntity>

    fun findByBranchIdAndKey(branchId: UUID?, key: String): SystemSettingEntity?
}
