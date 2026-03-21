package com.bialger.domain.inventory.mvc

import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.inventory.repository.InventoryCategoryRepository
import com.bialger.domain.mvc.InventoryItemMvcForm
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

@Controller("/mvc/inventory-items")
class InventoryItemMvcController(
    private val inventoryItemMvcService: InventoryItemMvcService,
    private val inventoryCategoryRepository: InventoryCategoryRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(InventoryItemMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "ТМЦ / склад (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/inventory-items/list",
            extra = mapOf(
                "items" to inventoryItemMvcService.listRows(),
                "sseEventsPath" to "/mvc/inventory-items/events",
                "sseEventName" to InventoryItemMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая позиция склада",
            activePage = "mvc",
            contentView = "pages/content/mvc/inventory-items/form-add",
            extra = mapOf(
                "categories" to inventoryCategoryRepository.findAllOrdered(),
                "branches" to branchRepository.findAllOrdered(),
                "rooms" to roomRepository.findAllOrdered()
            )
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: InventoryItemMvcForm): HttpResponse<Any> {
        return try {
            val cat = form.categoryId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите категорию")
            val br = form.branchId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите филиал")
            val room = form.roomId.parseUuidOrNull()
            val e = inventoryItemMvcService.create(
                cat,
                br,
                room,
                form.name,
                form.unit,
                InventoryItemMvcService.parseDecimal(form.quantity),
                InventoryItemMvcService.parseDecimalOpt(form.minQuantity),
                InventoryItemMvcService.parseDecimalOpt(form.costPrice)
            )
            HttpResponse.seeOther(URI.create("/mvc/inventory-items/${e.id}"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая позиция склада",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/inventory-items/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "categories" to inventoryCategoryRepository.findAllOrdered(),
                        "branches" to branchRepository.findAllOrdered(),
                        "rooms" to roomRepository.findAllOrdered()
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = inventoryItemMvcService.getById(id) ?: return HttpResponse.notFound()
        val row = inventoryItemMvcService.listRows().find { it.item.id == id }
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/inventory-items/detail",
                extra = mapOf(
                    "item" to item,
                    "categoryName" to (row?.categoryName ?: ""),
                    "branchName" to (row?.branchName ?: ""),
                    "roomName" to (row?.roomName ?: "")
                )
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = inventoryItemMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/inventory-items/form-edit",
                extra = mapOf(
                    "item" to item,
                    "categories" to inventoryCategoryRepository.findAllOrdered(),
                    "branches" to branchRepository.findAllOrdered(),
                    "rooms" to roomRepository.findAllOrdered()
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: InventoryItemMvcForm): HttpResponse<Any> {
        return try {
            val cat = form.categoryId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите категорию")
            val br = form.branchId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите филиал")
            val room = form.roomId.parseUuidOrNull()
            inventoryItemMvcService.update(
                id,
                cat,
                br,
                room,
                form.name,
                form.unit,
                InventoryItemMvcService.parseDecimal(form.quantity),
                InventoryItemMvcService.parseDecimalOpt(form.minQuantity),
                InventoryItemMvcService.parseDecimalOpt(form.costPrice)
            )
            HttpResponse.seeOther(URI.create("/mvc/inventory-items/$id"))
        } catch (e: Exception) {
            val item = inventoryItemMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/inventory-items/form-edit",
                    extra = mapOf(
                        "item" to item,
                        "categories" to inventoryCategoryRepository.findAllOrdered(),
                        "branches" to branchRepository.findAllOrdered(),
                        "rooms" to roomRepository.findAllOrdered(),
                        "error" to (e.message ?: "Ошибка")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            inventoryItemMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/inventory-items"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
