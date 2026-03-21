package com.bialger.domain.finance.mvc

import com.bialger.domain.finance.entity.SalaryRecordEntity
import com.bialger.domain.finance.repository.SalaryRecordRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Singleton
class SalaryRecordMvcService(
    private val salaryRecordRepository: SalaryRecordRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<SalaryRecordEntity> = salaryRecordRepository.findAllOrdered()

    fun getById(id: UUID): SalaryRecordEntity? = salaryRecordRepository.findById(id).orElse(null)

    fun create(
        employeeId: UUID,
        branchId: UUID,
        periodStart: LocalDate?,
        periodEnd: LocalDate?,
        hoursWorked: BigDecimal?,
        shiftsCount: Int?,
        amount: BigDecimal?
    ): SalaryRecordEntity {
        val id = UUID.randomUUID()
        val entity = SalaryRecordEntity(
            id = id,
            employeeId = employeeId,
            branchId = branchId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            hoursWorked = hoursWorked,
            shiftsCount = shiftsCount,
            amount = amount,
            createdAt = Instant.now()
        )
        salaryRecordRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), id.toString())
        return entity
    }

    fun update(
        id: UUID,
        employeeId: UUID,
        branchId: UUID,
        periodStart: LocalDate?,
        periodEnd: LocalDate?,
        hoursWorked: BigDecimal?,
        shiftsCount: Int?,
        amount: BigDecimal?
    ): SalaryRecordEntity {
        val existing = salaryRecordRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val updated = existing.copy(
            employeeId = employeeId,
            branchId = branchId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            hoursWorked = hoursWorked,
            shiftsCount = shiftsCount,
            amount = amount
        )
        salaryRecordRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), id.toString())
        return updated
    }

    fun delete(id: UUID) {
        salaryRecordRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        salaryRecordRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), id.toString())
    }

    companion object {
        const val TOPIC = "salary-records"
        const val EVENT_NAME = "salary-records-change"
    }
}
