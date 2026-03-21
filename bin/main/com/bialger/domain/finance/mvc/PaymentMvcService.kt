package com.bialger.domain.finance.mvc

import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.finance.entity.PaymentEntity
import com.bialger.domain.finance.enums.PaymentMethodType
import com.bialger.domain.finance.enums.PaymentStatusType
import com.bialger.domain.finance.repository.PaymentRepository
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PaymentListRow(
    val payment: PaymentEntity,
    val patientName: String
)

@Singleton
class PaymentMvcService(
    private val paymentRepository: PaymentRepository,
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listRows(): List<PaymentListRow> {
        val payments = paymentRepository.findAllOrdered()
        if (payments.isEmpty()) return emptyList()
        val apptIds = payments.map { it.appointmentId }.distinct()
        val apptMap =
            if (apptIds.isEmpty()) emptyMap()
            else appointmentRepository.findByIds(apptIds).associate { it.id to it }
        val patientIds = apptMap.values.map { it.patientId }.distinct()
        val patients =
            if (patientIds.isEmpty()) emptyMap()
            else patientRepository.findByIds(patientIds).associate { it.id to it.fullName }
        return payments.map { p ->
            val appt = apptMap[p.appointmentId]
            val patientName = appt?.let { patients[it.patientId] } ?: "—"
            PaymentListRow(p, patientName)
        }
    }

    fun getById(id: UUID): PaymentEntity? = paymentRepository.findById(id).orElse(null)

    fun create(
        appointmentId: UUID,
        amount: BigDecimal,
        paymentMethod: PaymentMethodType,
        paymentStatus: PaymentStatusType,
        notes: String?,
        createdBy: UUID
    ): PaymentEntity {
        require(appointmentRepository.findById(appointmentId).isPresent) { "Приём не найден" }
        require(employeeRepository.findById(createdBy).isPresent) { "Сотрудник не найден" }
        val id = UUID.randomUUID()
        val entity = PaymentEntity(
            id = id,
            appointmentId = appointmentId,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            createdBy = createdBy,
            createdAt = Instant.now()
        )
        paymentRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), amount.toPlainString())
        return entity
    }

    fun update(
        id: UUID,
        appointmentId: UUID,
        amount: BigDecimal,
        paymentMethod: PaymentMethodType,
        paymentStatus: PaymentStatusType,
        notes: String?,
        createdBy: UUID
    ): PaymentEntity {
        require(appointmentRepository.findById(appointmentId).isPresent) { "Приём не найден" }
        require(employeeRepository.findById(createdBy).isPresent) { "Сотрудник не найден" }
        val existing = paymentRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val updated = existing.copy(
            appointmentId = appointmentId,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            createdBy = createdBy
        )
        paymentRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), amount.toPlainString())
        return updated
    }

    fun delete(id: UUID) {
        val existing = paymentRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        paymentRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.appointmentId.toString())
    }

    companion object {
        const val TOPIC = "payments"
        const val EVENT_NAME = "payments-change"

        fun parseAmount(raw: String): BigDecimal {
            val t = raw.trim()
            require(t.isNotEmpty()) { "Укажите сумму" }
            return BigDecimal(t)
        }

        fun parsePaymentMethod(raw: String): PaymentMethodType =
            runCatching { PaymentMethodType.valueOf(raw.trim()) }
                .getOrElse { throw IllegalArgumentException("Некорректный способ оплаты") }

        fun parsePaymentStatus(raw: String): PaymentStatusType =
            runCatching { PaymentStatusType.valueOf(raw.trim()) }
                .getOrElse { throw IllegalArgumentException("Некорректный статус оплаты") }
    }
}
