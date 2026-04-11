package com.bialger.domain.attachment.mvc

import com.bialger.domain.attachment.enums.IntegrationType
import com.bialger.domain.mvc.IntegrationMvcForm
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
@Controller("/mvc/integrations")
class IntegrationMvcController(
    private val integrationMvcService: IntegrationMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(IntegrationMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Интеграции",
            activePage = "mvc",
            contentView = "pages/content/mvc/integrations/list",
            extra = mapOf(
                "items" to integrationMvcService.listAll(),
                "integrationTypes" to IntegrationType.values().toList(),
                "sseEventsPath" to "/mvc/integrations/events",
                "sseEventName" to IntegrationMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая интеграция",
            activePage = "mvc",
            contentView = "pages/content/mvc/integrations/form-add",
            extra = mapOf("integrationTypes" to IntegrationType.values().toList())
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: IntegrationMvcForm): HttpResponse<Any> {
        return try {
            val t = IntegrationMvcService.parseType(form.type)
            val active = IntegrationMvcService.parseActive(form.isActive)
            val e = integrationMvcService.create(t, form.name, form.config, active)
            HttpResponse.seeOther(URI.create("/mvc/integrations"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая интеграция",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/integrations/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "integrationTypes" to IntegrationType.values().toList()
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = integrationMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/integrations/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = integrationMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/integrations/form-edit",
                extra = mapOf(
                    "item" to item,
                    "integrationTypes" to IntegrationType.values().toList()
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: IntegrationMvcForm): HttpResponse<Any> {
        return try {
            val t = IntegrationMvcService.parseType(form.type)
            val active = IntegrationMvcService.parseActive(form.isActive)
            integrationMvcService.update(id, t, form.name, form.config, active)
            HttpResponse.seeOther(URI.create("/mvc/integrations"))
        } catch (e: Exception) {
            val item = integrationMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/integrations/form-edit",
                    extra = mapOf(
                        "item" to item,
                        "error" to (e.message ?: "Ошибка"),
                        "integrationTypes" to IntegrationType.values().toList()
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            integrationMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/integrations"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
