package com.bialger.domain.core.mvc

import com.bialger.domain.mvc.SpecialtyMvcForm
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

@Controller("/mvc/specialties")
class SpecialtyMvcController(
    private val specialtyMvcService: SpecialtyMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(SpecialtyMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Специализации (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/specialties/list",
            extra = mapOf(
                "items" to specialtyMvcService.listAll(),
                "sseEventsPath" to "/mvc/specialties/events",
                "sseEventName" to SpecialtyMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая специализация",
            activePage = "mvc",
            contentView = "pages/content/mvc/specialties/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: SpecialtyMvcForm): HttpResponse<Any> {
        return try {
            val e = specialtyMvcService.create(form.name, form.description)
            HttpResponse.seeOther(URI.create("/mvc/specialties/${e.id}"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая специализация",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/specialties/form-add",
                    extra = mapOf("error" to (e.message ?: "Validation error"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = specialtyMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/specialties/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = specialtyMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/specialties/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: SpecialtyMvcForm): HttpResponse<Any> {
        return try {
            specialtyMvcService.update(id, form.name, form.description)
            HttpResponse.seeOther(URI.create("/mvc/specialties/$id"))
        } catch (e: IllegalArgumentException) {
            val item = specialtyMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/specialties/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Validation error"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            specialtyMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/specialties"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
