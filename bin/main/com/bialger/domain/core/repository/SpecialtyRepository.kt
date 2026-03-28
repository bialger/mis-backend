package com.bialger.domain.core.repository

import com.bialger.domain.core.entity.SpecialtyEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SpecialtyRepository : CrudRepository<SpecialtyEntity, UUID> {

    @Query("SELECT * FROM specialty ORDER BY name")
    fun findAllOrdered(): List<SpecialtyEntity>

    @Query("SELECT * FROM specialty WHERE id IN (:ids)")
    fun findByIds(ids: List<UUID>): List<SpecialtyEntity>

    fun findByName(name: String): SpecialtyEntity?
}
