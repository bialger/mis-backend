package com.bialger.domain.laboratory.mvc

import com.bialger.domain.mvc.LaboratoryMvcForm
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

@Controller("/mvc/laboratories")
class LaboratoryMvcController(
    private val laboratoryMvcService: LaboratoryMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(LaboratoryMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Лаборатории",
            activePage = "mvc",
            contentView = "pages/content/mvc/laboratories/list",
            extra = mapOf(
                "items" to laboratoryMvcService.listAll(),
                "sseEventsPath" to "/mvc/laboratories/events",
                "sseEventName" to LaboratoryMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая лаборатория",
            activePage = "mvc",
            contentView = "pages/content/mvc/laboratories/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: LaboratoryMvcForm): HttpResponse<Any> {
        return try {
            val active = LaboratoryMvcService.parseActive(form.isActive)
            val e = laboratoryMvcService.create(form.name, form.integrationType, form.config, active)
            HttpResponse.seeOther(URI.create("/mvc/laboratories/${e.id}"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая лаборатория",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/laboratories/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = laboratoryMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/laboratories/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = laboratoryMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/laboratories/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: LaboratoryMvcForm): HttpResponse<Any> {
        return try {
            val active = LaboratoryMvcService.parseActive(form.isActive)
            laboratoryMvcService.update(id, form.name, form.integrationType, form.config, active)
            HttpResponse.seeOther(URI.create("/mvc/laboratories/$id"))
        } catch (e: Exception) {
            val item = laboratoryMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/laboratories/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            laboratoryMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/laboratories"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
