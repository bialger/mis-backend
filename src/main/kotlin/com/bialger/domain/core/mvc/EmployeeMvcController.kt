package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.mvc.EmployeeMvcForm
import com.bialger.domain.mvc.formCheckboxOn
import com.bialger.domain.mvc.parseUuidList
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
import io.swagger.v3.oas.annotations.Hidden

@Hidden
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
            contentView = "pages/content/mvc/employees/form-add",
            extra = employeeMvcService.formExtras(null)
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: EmployeeMvcForm): HttpResponse<Any> {
        return try {
            val roleId = form.roleId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите роль")
            val e = employeeMvcService.create(
                form.fullName,
                form.email,
                form.phone,
                form.password,
                form.isActive.formCheckboxOn(),
                form.specialtyIds.parseUuidList(),
                form.branchIds.parseUuidList(),
                roleId
            )
            HttpResponse.seeOther(URI.create("/mvc/employees"))
        } catch (e: IllegalArgumentException) {
            val extra = employeeMvcService.formExtras(null).toMutableMap()
            extra["error"] = e.message ?: "Ошибка"
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый сотрудник",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/employees/form-add",
                    extra = extra
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val detail = employeeMvcService.detailExtras(id) ?: return HttpResponse.notFound()
        val item = detail["item"] as EmployeeEntity
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.fullName,
                activePage = "mvc",
                contentView = "pages/content/mvc/employees/detail",
                extra = detail
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = employeeMvcService.getById(id) ?: return HttpResponse.notFound()
        val extra = employeeMvcService.formExtras(id).toMutableMap()
        extra["item"] = item
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.fullName}",
                activePage = "mvc",
                contentView = "pages/content/mvc/employees/form-edit",
                extra = extra
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: EmployeeMvcForm): HttpResponse<Any> {
        return try {
            val roleId = form.roleId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите роль")
            employeeMvcService.update(
                id,
                form.fullName,
                form.email,
                form.phone,
                form.password.takeIf { it.isNotBlank() },
                form.isActive.formCheckboxOn(),
                form.specialtyIds.parseUuidList(),
                form.branchIds.parseUuidList(),
                roleId
            )
            HttpResponse.seeOther(URI.create("/mvc/employees"))
        } catch (e: IllegalArgumentException) {
            val item = employeeMvcService.getById(id) ?: return HttpResponse.notFound()
            val extra = employeeMvcService.formExtras(id).toMutableMap()
            extra["item"] = item
            extra["error"] = e.message ?: "Ошибка"
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.fullName}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/employees/form-edit",
                    extra = extra
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
