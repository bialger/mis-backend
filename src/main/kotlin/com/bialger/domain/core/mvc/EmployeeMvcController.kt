package com.bialger.domain.core.mvc

import com.bialger.domain.mvc.EmployeeMvcForm
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

@Controller("/mvc/employees")
class EmployeeMvcController(
    private val employeeMvcService: EmployeeMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(EmployeeMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Сотрудники (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/employees/list",
            extra = mapOf(
                "items" to employeeMvcService.listAll(),
                "sseEventsPath" to "/mvc/employees/events",
                "sseEventName" to EmployeeMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый сотрудник",
            activePage = "mvc",
            contentView = "pages/content/mvc/employees/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: EmployeeMvcForm): HttpResponse<Any> {
        return try {
            val e = employeeMvcService.create(
                form.fullName,
                form.email,
                form.phone,
                form.password,
                form.isActive.formCheckboxOn()
            )
            HttpResponse.seeOther(URI.create("/mvc/employees/${e.id}"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый сотрудник",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/employees/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = employeeMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.fullName,
                activePage = "mvc",
                contentView = "pages/content/mvc/employees/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = employeeMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.fullName}",
                activePage = "mvc",
                contentView = "pages/content/mvc/employees/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: EmployeeMvcForm): HttpResponse<Any> {
        return try {
            employeeMvcService.update(
                id,
                form.fullName,
                form.email,
                form.phone,
                form.password,
                form.isActive.formCheckboxOn()
            )
            HttpResponse.seeOther(URI.create("/mvc/employees/$id"))
        } catch (e: IllegalArgumentException) {
            val item = employeeMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.fullName}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/employees/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            employeeMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/employees"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
