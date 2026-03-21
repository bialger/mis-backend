package com.bialger.domain.core.mvc

import com.bialger.domain.mvc.PermissionMvcForm
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

@Controller("/mvc/permissions")
class PermissionMvcController(
    private val permissionMvcService: PermissionMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(PermissionMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Права (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/permissions/list",
            extra = mapOf(
                "items" to permissionMvcService.listAll(),
                "sseEventsPath" to "/mvc/permissions/events",
                "sseEventName" to PermissionMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новое право",
            activePage = "mvc",
            contentView = "pages/content/mvc/permissions/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: PermissionMvcForm): HttpResponse<Any> {
        return try {
            val e = permissionMvcService.create(form.code, form.name, form.description)
            HttpResponse.seeOther(URI.create("/mvc/permissions/${e.id}"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новое право",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/permissions/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = permissionMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.code,
                activePage = "mvc",
                contentView = "pages/content/mvc/permissions/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = permissionMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.code}",
                activePage = "mvc",
                contentView = "pages/content/mvc/permissions/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: PermissionMvcForm): HttpResponse<Any> {
        return try {
            permissionMvcService.update(id, form.code, form.name, form.description)
            HttpResponse.seeOther(URI.create("/mvc/permissions/$id"))
        } catch (e: IllegalArgumentException) {
            val item = permissionMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.code}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/permissions/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            permissionMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/permissions"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
