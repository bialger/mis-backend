package com.bialger.domain.clinical.mvc

import com.bialger.domain.mvc.ClinicalTemplateMvcForm
import com.bialger.domain.mvc.parseUuidOrNull
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

@Controller("/mvc/templates")
class ClinicalTemplateMvcController(
    private val clinicalTemplateMvcService: ClinicalTemplateMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

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
            extra = mapOf("templateTypes" to com.bialger.domain.clinical.enums.TemplateType.values().toList())
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
            HttpResponse.redirect(URI.create("/mvc/templates/${e.id}"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый шаблон",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/templates/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "templateTypes" to com.bialger.domain.clinical.enums.TemplateType.values().toList()
                    )
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
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/templates/form-edit",
                extra = mapOf(
                    "item" to item,
                    "templateTypes" to com.bialger.domain.clinical.enums.TemplateType.values().toList()
                )
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
            HttpResponse.redirect(URI.create("/mvc/templates/$id"))
        } catch (e: Exception) {
            val item = clinicalTemplateMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/templates/form-edit",
                    extra = mapOf(
                        "item" to item,
                        "error" to (e.message ?: "Ошибка"),
                        "templateTypes" to com.bialger.domain.clinical.enums.TemplateType.values().toList()
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            clinicalTemplateMvcService.delete(id)
            HttpResponse.redirect(URI.create("/mvc/templates"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
