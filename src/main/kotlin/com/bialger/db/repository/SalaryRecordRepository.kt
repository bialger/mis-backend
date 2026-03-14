package com.bialger.db.repository

import com.bialger.db.entity.SalaryRecordEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SalaryRecordRepository : CrudRepository<SalaryRecordEntity, UUID> {

    fun findByEmployeeId(employeeId: UUID): List<SalaryRecordEntity>

    fun findByBranchId(branchId: UUID): List<SalaryRecordEntity>
}
