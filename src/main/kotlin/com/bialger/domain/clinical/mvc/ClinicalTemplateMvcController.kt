package com.bialger.domain.clinical.mvc

import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.SpecialtyRepository
import com.bialger.domain.mvc.ClinicalTemplateMvcForm
import com.bialger.domain.mvc.employeeDropdown
import com.bialger.domain.mvc.parseUuidOrNull
import com.bialger.domain.mvc.specialtyDropdown
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
import java.util.UUID
import io.swagger.v3.oas.annotations.Hidden

@Hidden
@Controller("/mvc/templates")
class ClinicalTemplateMvcController(
    private val clinicalTemplateMvcService: ClinicalTemplateMvcService,
    private val specialtyRepository: SpecialtyRepository,
    private val employeeRepository: EmployeeRepository,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    private fun templateFormExtras(specialtyEnsureId: UUID?, employeeEnsureId: UUID?) = mapOf(
        "templateTypes" to com.bialger.domain.clinical.enums.TemplateType.values().toList(),
        "specialties" to specialtyDropdown(specialtyRepository, specialtyEnsureId),
        "employees" to employeeDropdown(employeeRepository, employeeEnsureId)
    )

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(ClinicalTemplateMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Шаблоны документов",
            activePage = "mvc",
            contentView = "pages/content/mvc/templates/list",
            extra = mapOf(
                "items" to clinicalTemplateMvcService.listAll(),
                "templateTypes" to com.bialger.domain.clinical.enums.TemplateType.values().toList(),
                "sseEventsPath" to "/mvc/templates/events",
                "sseEventName" to ClinicalTemplateMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый шаблон",
            activePage = "mvc",
            contentView = "pages/content/mvc/templates/form-add",
            extra = templateFormExtras(null, null)
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: ClinicalTemplateMvcForm): HttpResponse<Any> {
        return try {
            val type = ClinicalTemplateMvcService.parseType(form.type)
            val spec = form.specialtyId.parseUuidOrNull()
            val emp = form.employeeId.parseUuidOrNull()
            val active = ClinicalTemplateMvcService.parseActive(form.isActive)
            val e = clinicalTemplateMvcService.create(form.name, type, spec, emp, form.content, active)
            HttpResponse.seeOther(URI.create("/mvc/templates"))
        } catch (e: Exception) {
            val extra = mutableMapOf<String, Any>()
            extra.putAll(templateFormExtras(null, null))
            extra["error"] = e.message ?: "Ошибка"
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый шаблон",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/templates/form-add",
                    extra = extra
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = clinicalTemplateMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/templates/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = clinicalTemplateMvcService.getById(id) ?: return HttpResponse.notFound()
        val editExtra = mutableMapOf<String, Any>()
        editExtra.putAll(templateFormExtras(item.specialtyId, item.employeeId))
        editExtra["item"] = item
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/templates/form-edit",
                extra = editExtra
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: ClinicalTemplateMvcForm): HttpResponse<Any> {
        return try {
            val type = ClinicalTemplateMvcService.parseType(form.type)
            val spec = form.specialtyId.parseUuidOrNull()
            val emp = form.employeeId.parseUuidOrNull()
            val active = ClinicalTemplateMvcService.parseActive(form.isActive)
            clinicalTemplateMvcService.update(id, form.name, type, spec, emp, form.content, active)
            HttpResponse.seeOther(URI.create("/mvc/templates"))
        } catch (e: Exception) {
            val item = clinicalTemplateMvcService.getById(id) ?: return HttpResponse.notFound()
            val extra = mutableMapOf<String, Any>()
            extra.putAll(templateFormExtras(item.specialtyId, item.employeeId))
            extra["item"] = item
            extra["error"] = e.message ?: "Ошибка"
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/templates/form-edit",
                    extra = extra
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            clinicalTemplateMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/templates"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
