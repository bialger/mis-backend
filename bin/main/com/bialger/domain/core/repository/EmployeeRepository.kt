package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.EmployeeEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface EmployeeRepository : CrudRepository<EmployeeEntity, UUID> {

    @Query("SELECT * FROM employee ORDER BY full_name")
    fun findAllOrdered(): List<EmployeeEntity>

    @Query("SELECT * FROM employee ORDER BY full_name LIMIT :limit")
    fun findTopOrdered(limit: Int): List<EmployeeEntity>

    @Query("SELECT * FROM employee WHERE id IN (:ids)")
    fun findByIds(ids: List<UUID>): List<EmployeeEntity>

    fun findByEmail(email: String): EmployeeEntity?

    fun findByIsActive(isActive: Boolean): List<EmployeeEntity>

    fun existsByEmail(email: String): Boolean
}
