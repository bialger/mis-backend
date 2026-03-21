package com.bialger.domain.laboratory.repository

import com.bialger.domain.laboratory.entity.LabOrderItemEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface LabOrderItemRepository : CrudRepository<LabOrderItemEntity, UUID> {

    fun findByLabOrderId(labOrderId: UUID): List<LabOrderItemEntity>

    fun findByLabTestId(labTestId: UUID): List<LabOrderItemEntity>
}
