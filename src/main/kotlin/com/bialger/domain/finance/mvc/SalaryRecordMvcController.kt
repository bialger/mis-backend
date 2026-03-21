package com.bialger.domain.finance.mvc

import com.bialger.domain.mvc.SalaryRecordMvcForm
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
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Controller("/mvc/salary-records")
class SalaryRecordMvcController(
    private val salaryRecordMvcService: SalaryRecordMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(SalaryRecordMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Записи зарплаты",
            activePage = "mvc",
            contentView = "pages/content/mvc/salary-records/list",
            extra = mapOf(
                "items" to salaryRecordMvcService.listAll(),
                "sseEventsPath" to "/mvc/salary-records/events",
                "sseEventName" to SalaryRecordMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая запись зарплаты",
            activePage = "mvc",
            contentView = "pages/content/mvc/salary-records/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: SalaryRecordMvcForm): HttpResponse<Any> {
        return try {
            val e = salaryRecordMvcService.create(
                UUID.fromString(form.employeeId.trim()),
                UUID.fromString(form.branchId.trim()),
                form.periodStart.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                form.periodEnd.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                form.hoursWorked.trim().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) },
                form.shiftsCount.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
                form.amount.trim().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) }
            )
            HttpResponse.seeOther(URI.create("/mvc/salary-records/${e.id}"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая запись зарплаты",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/salary-records/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = salaryRecordMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Зарплата ${item.id}",
                activePage = "mvc",
                contentView = "pages/content/mvc/salary-records/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = salaryRecordMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование записи",
                activePage = "mvc",
                contentView = "pages/content/mvc/salary-records/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: SalaryRecordMvcForm): HttpResponse<Any> {
        return try {
            salaryRecordMvcService.update(
                id,
                UUID.fromString(form.employeeId.trim()),
                UUID.fromString(form.branchId.trim()),
                form.periodStart.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                form.periodEnd.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
                form.hoursWorked.trim().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) },
                form.shiftsCount.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
                form.amount.trim().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) }
            )
            HttpResponse.seeOther(URI.create("/mvc/salary-records/$id"))
        } catch (e: Exception) {
            val item = salaryRecordMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование записи",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/salary-records/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            salaryRecordMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/salary-records"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
