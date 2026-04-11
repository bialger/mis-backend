package com.bialger.api

import com.bialger.api.dto.PaymentPatchDto
import com.bialger.api.dto.PaymentRestDto
import com.bialger.domain.finance.entity.PaymentEntity
import com.bialger.domain.finance.enums.PaymentStatusType
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
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of payments",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PaymentRestDto::class))])
    )
    fun list(@QueryValue("appointmentId") appointmentId: UUID?): List<PaymentRestDto> {
        val list = if (appointmentId != null) {
            paymentRepository.findByAppointmentId(appointmentId)
        } else {
            paymentRepository.findAllOrdered()
        }
        return list.map { toDto(it) }
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update payment")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated payment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PaymentRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation or business error"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    open fun update(@PathVariable id: UUID, @Valid @io.micronaut.http.annotation.Body dto: PaymentPatchDto): PaymentRestDto {
        val paidAmount = dto.paidAmount?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { java.math.BigDecimal(it) }.getOrElse { null } }
        val e = paymentMvcService.update(
            id = id,
            appointmentId = dto.appointmentId?.parseUuidOrNull()
                ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "appointmentId required"),
            amount = PaymentMvcService.parseAmount(dto.amount ?: ""),
            paidAmount = paidAmount,
            paymentMethod = PaymentMvcService.parsePaymentMethod(dto.paymentMethod ?: ""),
            paymentStatus = PaymentMvcService.parsePaymentStatus(dto.paymentStatus ?: ""),
            notes = dto.notes,
            createdBy = dto.createdBy?.parseUuidOrNull()
                ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "createdBy required")
        )
        return toDto(e)
    }

    private fun toDto(p: PaymentEntity): PaymentRestDto {
        val total = p.amount.toDouble()
        val paid = p.paidAmount?.toDouble() ?: when (p.paymentStatus) {
            PaymentStatusType.PAID -> total
            PaymentStatusType.PARTIAL -> total * 0.5
            else -> 0.0
        }
        return PaymentRestDto(
            id = p.id.toString(),
            appointmentId = p.appointmentId.toString(),
            amount = p.amount.toPlainString(),
            paidAmount = p.paidAmount?.toPlainString(),
            total = total,
            paid = paid,
            paymentMethod = p.paymentMethod.name,
            paymentStatus = p.paymentStatus.name,
            method = p.paymentMethod.name,
            notes = p.notes ?: "",
            createdBy = p.createdBy.toString()
        )
    }
}
