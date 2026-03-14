package com.bialger.db.repository

import com.bialger.db.entity.RoomEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface RoomRepository : CrudRepository<RoomEntity, UUID> {

    fun findByBranchId(branchId: UUID): List<RoomEntity>

    fun findByBranchIdAndIsActive(branchId: UUID, isActive: Boolean): List<RoomEntity>
}
