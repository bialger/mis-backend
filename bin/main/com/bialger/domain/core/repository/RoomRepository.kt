package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.RoomEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface RoomRepository : CrudRepository<RoomEntity, UUID> {

    @Query("SELECT * FROM room ORDER BY name")
    fun findAllOrdered(): List<RoomEntity>

    @Query("SELECT * FROM room WHERE id IN (:ids)")
    fun findByIds(ids: List<UUID>): List<RoomEntity>

    fun findByBranchId(branchId: UUID): List<RoomEntity>

    fun findByBranchIdAndIsActive(branchId: UUID, isActive: Boolean): List<RoomEntity>
}
