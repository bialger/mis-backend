package com.bialger.domain.core.mvc

import com.bialger.domain.mvc.OrganizationMvcForm
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

@Controller("/mvc/organizations")
class OrganizationMvcController(
    private val organizationMvcService: OrganizationMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(OrganizationMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Организации (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/organizations/list",
            extra = mapOf(
                "items" to organizationMvcService.listAll(),
                "sseEventsPath" to "/mvc/organizations/events",
                "sseEventName" to OrganizationMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая организация",
            activePage = "mvc",
            contentView = "pages/content/mvc/organizations/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: OrganizationMvcForm): HttpResponse<Any> {
        return try {
            val e = organizationMvcService.create(form.name, form.codeOkpo, form.codeOkud, form.address)
            HttpResponse.seeOther(URI.create("/mvc/organizations/${e.id}"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая организация",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/organizations/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = organizationMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/organizations/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = organizationMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/organizations/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: OrganizationMvcForm): HttpResponse<Any> {
        return try {
            organizationMvcService.update(id, form.name, form.codeOkpo, form.codeOkud, form.address)
            HttpResponse.seeOther(URI.create("/mvc/organizations/$id"))
        } catch (e: IllegalArgumentException) {
            val item = organizationMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/organizations/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            organizationMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/organizations"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
