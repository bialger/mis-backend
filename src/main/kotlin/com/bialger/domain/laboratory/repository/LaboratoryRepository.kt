package com.bialger.domain.laboratory.repository

import com.bialger.domain.laboratory.entity.LaboratoryEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface LaboratoryRepository : CrudRepository<LaboratoryEntity, UUID> {

    @Query("SELECT * FROM laboratory ORDER BY name")
    fun findAllOrdered(): List<LaboratoryEntity>

    fun findByName(name: String): LaboratoryEntity?

    fun findByIsActive(isActive: Boolean): List<LaboratoryEntity>
}
