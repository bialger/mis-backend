package com.bialger.api

import com.bialger.api.dto.PaymentPatchDto
import com.bialger.domain.finance.entity.PaymentEntity
import com.bialger.domain.finance.mvc.PaymentMvcService
import com.bialger.domain.finance.repository.PaymentRepository
import com.bialger.domain.mvc.parseUuidOrNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/payments")
@Tag(name = "Payments", description = "Payments linked to appointments")
open class PaymentsApiController(
    private val paymentRepository: PaymentRepository,
    private val paymentMvcService: PaymentMvcService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List payments, optionally filtered by appointment")
    fun list(@QueryValue("appointmentId") appointmentId: UUID?): List<Map<String, Any?>> {
        val list =
            if (appointmentId != null) {
                paymentRepository.findByAppointmentId(appointmentId)
            } else {
                paymentRepository.findAllOrdered()
            }
        return list.map { paymentVm(it) }
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update payment")
    open fun update(@PathVariable id: UUID, @Valid @io.micronaut.http.annotation.Body dto: PaymentPatchDto): Map<String, Any?> {
        val e = paymentMvcService.update(
            id = id,
            appointmentId = dto.appointmentId?.parseUuidOrNull()
                ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "appointmentId required"),
            amount = PaymentMvcService.parseAmount(dto.amount ?: ""),
            paymentMethod = PaymentMvcService.parsePaymentMethod(dto.paymentMethod ?: ""),
            paymentStatus = PaymentMvcService.parsePaymentStatus(dto.paymentStatus ?: ""),
            notes = dto.notes,
            createdBy = dto.createdBy?.parseUuidOrNull()
                ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "createdBy required")
        )
        return paymentVm(e)
    }

    private fun paymentVm(p: PaymentEntity): Map<String, Any?> {
        val total = p.amount.toDouble()
        val paid =
            when (p.paymentStatus) {
                com.bialger.domain.finance.enums.PaymentStatusType.PAID -> total
                com.bialger.domain.finance.enums.PaymentStatusType.PARTIAL -> total * 0.5
                else -> 0.0
            }
        return mapOf(
            "id" to p.id.toString(),
            "appointmentId" to p.appointmentId.toString(),
            "amount" to p.amount.toPlainString(),
            "total" to total,
            "paid" to paid,
            "paymentMethod" to p.paymentMethod.name,
            "paymentStatus" to p.paymentStatus.name,
            "method" to p.paymentMethod.name,
            "notes" to (p.notes ?: ""),
            "createdBy" to p.createdBy.toString()
        )
    }
}
