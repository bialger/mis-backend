package com.bialger.domain.finance.mvc

import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.finance.enums.PaymentMethodType
import com.bialger.domain.finance.enums.PaymentStatusType
import com.bialger.domain.mvc.MvcFormListLimits
import com.bialger.domain.mvc.PaymentMvcForm
import com.bialger.domain.mvc.employeeDropdown
import com.bialger.domain.mvc.parseUuidOrNull
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.web.AppPageModelFactory
import com.bialger.web.DomainEventSseHub
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.sse.Event
import io.micronaut.views.ModelAndView
import org.reactivestreams.Publisher
import java.net.URI
import java.time.Instant
import java.util.UUID
import io.swagger.v3.oas.annotations.Hidden

@Hidden
@Controller("/mvc/payments")
class PaymentMvcController(
    private val paymentMvcService: PaymentMvcService,
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(PaymentMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Платежи (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/payments/list",
            extra = mapOf(
                "rows" to paymentMvcService.listRows(),
                "sseEventsPath" to "/mvc/payments/events",
                "sseEventName" to PaymentMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый платёж",
            activePage = "mvc",
            contentView = "pages/content/mvc/payments/form-add",
            extra = mapOf(
                "appointmentOptions" to appointmentOptions(null),
                "employees" to employeeDropdown(employeeRepository, null),
                "paymentMethods" to PaymentMethodType.entries,
                "paymentStatuses" to PaymentStatusType.entries
            )
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: PaymentMvcForm): HttpResponse<Any> {
        return try {
            val apptId = form.appointmentId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите приём")
            val createdBy = form.createdBy.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите сотрудника")
            val e = paymentMvcService.create(
                appointmentId = apptId,
                amount = PaymentMvcService.parseAmount(form.amount),
                paymentMethod = PaymentMvcService.parsePaymentMethod(form.paymentMethod),
                paymentStatus = PaymentMvcService.parsePaymentStatus(form.paymentStatus),
                notes = form.notes,
                createdBy = createdBy
            )
            HttpResponse.seeOther(URI.create("/mvc/payments"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый платёж",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/payments/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "appointmentOptions" to appointmentOptions(null),
                        "employees" to employeeDropdown(employeeRepository, null),
                        "paymentMethods" to PaymentMethodType.entries,
                        "paymentStatuses" to PaymentStatusType.entries
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val p = paymentMvcService.getById(id) ?: return HttpResponse.notFound()
        val row = paymentMvcService.listRows().find { it.payment.id == id }
        val createdByName = employeeRepository.findById(p.createdBy).map { it.fullName }.orElse(p.createdBy.toString())
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Платёж ${p.amount}",
                activePage = "mvc",
                contentView = "pages/content/mvc/payments/detail",
                extra = mapOf(
                    "payment" to p,
                    "patientName" to (row?.patientName ?: ""),
                    "createdByName" to createdByName
                )
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val p = paymentMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование платежа",
                activePage = "mvc",
                contentView = "pages/content/mvc/payments/form-edit",
                extra = mapOf(
                    "payment" to p,
                    "appointmentOptions" to appointmentOptions(p.appointmentId),
                    "employees" to employeeDropdown(employeeRepository, p.createdBy),
                    "paymentMethods" to PaymentMethodType.entries,
                    "paymentStatuses" to PaymentStatusType.entries
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: PaymentMvcForm): HttpResponse<Any> {
        return try {
            val apptId = form.appointmentId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите приём")
            val createdBy = form.createdBy.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите сотрудника")
            paymentMvcService.update(
                id = id,
                appointmentId = apptId,
                amount = PaymentMvcService.parseAmount(form.amount),
                paymentMethod = PaymentMvcService.parsePaymentMethod(form.paymentMethod),
                paymentStatus = PaymentMvcService.parsePaymentStatus(form.paymentStatus),
                notes = form.notes,
                createdBy = createdBy
            )
            HttpResponse.seeOther(URI.create("/mvc/payments"))
        } catch (e: Exception) {
            val p = paymentMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование платежа",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/payments/form-edit",
                    extra = mapOf(
                        "payment" to p,
                        "appointmentOptions" to appointmentOptions(p.appointmentId),
                        "employees" to employeeDropdown(employeeRepository, p.createdBy),
                        "paymentMethods" to PaymentMethodType.entries,
                        "paymentStatuses" to PaymentStatusType.entries,
                        "error" to (e.message ?: "Ошибка")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            paymentMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/payments"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }

    private fun appointmentOptions(ensureAppointmentId: UUID?): List<Map<String, Any>> {
        val limit = MvcFormListLimits.APPOINTMENTS_FOR_PAYMENT
        var appts: List<AppointmentEntity> = appointmentRepository.findTopOrdered(limit)
        val ensureId = ensureAppointmentId
        if (ensureId != null && appts.none { it.id == ensureId }) {
            val extra = appointmentRepository.findById(ensureId).orElse(null)
            if (extra != null) appts = appts + extra
        }
        val patientIds = appts.map { it.patientId }.distinct()
        val patients =
            if (patientIds.isEmpty()) emptyMap()
            else patientRepository.findByIds(patientIds).associate { it.id to it.fullName }
        return appts
            .sortedWith(
                compareByDescending<AppointmentEntity> { it.createdAt ?: Instant.EPOCH }
                    .thenBy { it.id }
            )
            .map { a ->
                mapOf(
                    "id" to a.id,
                    "label" to "${patients[a.patientId] ?: "—"} · ${a.status}"
                )
            }
    }
}
