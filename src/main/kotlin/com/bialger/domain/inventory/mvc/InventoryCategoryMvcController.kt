package com.bialger.domain.inventory.mvc

import com.bialger.domain.mvc.InventoryCategoryMvcForm
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
@Controller("/mvc/inventory-categories")
class InventoryCategoryMvcController(
    private val inventoryCategoryMvcService: InventoryCategoryMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(InventoryCategoryMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Категории склада",
            activePage = "mvc",
            contentView = "pages/content/mvc/inventory-categories/list",
            extra = mapOf(
                "items" to inventoryCategoryMvcService.listAll(),
                "sseEventsPath" to "/mvc/inventory-categories/events",
                "sseEventName" to InventoryCategoryMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая категория",
            activePage = "mvc",
            contentView = "pages/content/mvc/inventory-categories/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: InventoryCategoryMvcForm): HttpResponse<Any> {
        return try {
            val e = inventoryCategoryMvcService.create(form.name, form.description)
            HttpResponse.seeOther(URI.create("/mvc/inventory-categories"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая категория",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/inventory-categories/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = inventoryCategoryMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/inventory-categories/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = inventoryCategoryMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/inventory-categories/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: InventoryCategoryMvcForm): HttpResponse<Any> {
        return try {
            inventoryCategoryMvcService.update(id, form.name, form.description)
            HttpResponse.seeOther(URI.create("/mvc/inventory-categories"))
        } catch (e: IllegalArgumentException) {
            val item = inventoryCategoryMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/inventory-categories/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            inventoryCategoryMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/inventory-categories"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
