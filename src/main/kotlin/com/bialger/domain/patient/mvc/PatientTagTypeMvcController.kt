package com.bialger.domain.patient.mvc

import com.bialger.domain.mvc.PatientTagTypeMvcForm
import com.bialger.domain.mvc.formCheckboxOn
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
@Controller("/mvc/patient-tag-types")
class PatientTagTypeMvcController(
    private val patientTagTypeMvcService: PatientTagTypeMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(PatientTagTypeMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Типы тегов пациента",
            activePage = "mvc",
            contentView = "pages/content/mvc/patient-tag-types/list",
            extra = mapOf(
                "items" to patientTagTypeMvcService.listAll(),
                "sseEventsPath" to "/mvc/patient-tag-types/events",
                "sseEventName" to PatientTagTypeMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый тип тега",
            activePage = "mvc",
            contentView = "pages/content/mvc/patient-tag-types/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: PatientTagTypeMvcForm): HttpResponse<Any> {
        return try {
            val active = form.isActive.formCheckboxOn()
            val e = patientTagTypeMvcService.create(form.code, form.name, form.icon, form.description, active)
            HttpResponse.seeOther(URI.create("/mvc/patient-tag-types"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый тип тега",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/patient-tag-types/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = patientTagTypeMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/patient-tag-types/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = patientTagTypeMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/patient-tag-types/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: PatientTagTypeMvcForm): HttpResponse<Any> {
        return try {
            val active = form.isActive.formCheckboxOn()
            patientTagTypeMvcService.update(id, form.code, form.name, form.icon, form.description, active)
            HttpResponse.seeOther(URI.create("/mvc/patient-tag-types"))
        } catch (e: IllegalArgumentException) {
            val item = patientTagTypeMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/patient-tag-types/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            patientTagTypeMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/patient-tag-types"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
