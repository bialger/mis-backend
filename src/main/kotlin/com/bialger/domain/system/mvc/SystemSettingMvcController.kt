package com.bialger.domain.system.mvc

import com.bialger.domain.mvc.SystemSettingMvcForm
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

@Controller("/mvc/system-settings")
class SystemSettingMvcController(
    private val systemSettingMvcService: SystemSettingMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(SystemSettingMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Системные настройки",
            activePage = "mvc",
            contentView = "pages/content/mvc/system-settings/list",
            extra = mapOf(
                "items" to systemSettingMvcService.listAll(),
                "sseEventsPath" to "/mvc/system-settings/events",
                "sseEventName" to SystemSettingMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая настройка",
            activePage = "mvc",
            contentView = "pages/content/mvc/system-settings/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: SystemSettingMvcForm): HttpResponse<Any> {
        return try {
            val branch = SystemSettingMvcService.parseBranchId(form.branchId)
            val e = systemSettingMvcService.create(branch, form.key, form.value, form.description)
            HttpResponse.seeOther(URI.create("/mvc/system-settings/${e.id}"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая настройка",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/system-settings/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = systemSettingMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.key,
                activePage = "mvc",
                contentView = "pages/content/mvc/system-settings/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = systemSettingMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.key}",
                activePage = "mvc",
                contentView = "pages/content/mvc/system-settings/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: SystemSettingMvcForm): HttpResponse<Any> {
        return try {
            val branch = SystemSettingMvcService.parseBranchId(form.branchId)
            systemSettingMvcService.update(id, branch, form.key, form.value, form.description)
            HttpResponse.seeOther(URI.create("/mvc/system-settings/$id"))
        } catch (e: Exception) {
            val item = systemSettingMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.key}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/system-settings/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            systemSettingMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/system-settings"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
