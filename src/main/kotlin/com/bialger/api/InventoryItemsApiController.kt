package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import com.bialger.api.dto.InventoryItemCreateDto
import com.bialger.api.dto.InventoryItemUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.inventory.mvc.InventoryItemMvcService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/inventory-items")
@Tag(name = "Inventory", description = "Склад (ТМЦ)")
open class InventoryItemsApiController(
    private val inventoryItemMvcService: InventoryItemMvcService,
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Список позиций")
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<Map<String, Any?>>> {
        val rows = crmShellApplicationService.inventoryItemMaps()
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Позиция по id")
    fun getOne(@PathVariable id: UUID): Map<String, Any?> {
        val row = inventoryItemMvcService.listRows().find { it.item.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return mapOf(
            "id" to row.item.id.toString(),
            "name" to row.item.name,
            "branchId" to row.item.branchId.toString(),
            "quantity" to row.item.quantity,
            "unit" to row.item.unit,
            "minQuantity" to row.item.minQuantity,
            "categoryName" to row.categoryName,
            "roomName" to (row.roomName ?: "")
        )
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Создать позицию")
    open fun create(@Body @Valid dto: InventoryItemCreateDto): Map<String, Any?> {
        val e = inventoryItemMvcService.create(
            categoryId = dto.categoryId,
            branchId = dto.branchId,
            roomId = dto.roomId,
            name = dto.name,
            unit = dto.unit,
            quantity = dto.quantity,
            minQuantity = dto.minQuantity,
            costPrice = dto.costPrice
        )
        return crmShellApplicationService.inventoryItemMaps().find { (it["id"] as String) == e.id.toString() }
            ?: throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load item")
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Обновить позицию")
    open fun update(@PathVariable id: UUID, @Body @Valid dto: InventoryItemUpdateDto): Map<String, Any?> {
        inventoryItemMvcService.update(
            id = id,
            categoryId = dto.categoryId,
            branchId = dto.branchId,
            roomId = dto.roomId,
            name = dto.name,
            unit = dto.unit,
            quantity = dto.quantity,
            minQuantity = dto.minQuantity,
            costPrice = dto.costPrice
        )
        val row = inventoryItemMvcService.listRows().find { it.item.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return mapOf(
            "id" to row.item.id.toString(),
            "name" to row.item.name,
            "branchId" to row.item.branchId.toString(),
            "quantity" to row.item.quantity,
            "unit" to row.item.unit,
            "minQuantity" to row.item.minQuantity,
            "categoryName" to row.categoryName,
            "roomName" to (row.roomName ?: "")
        )
    }

    @Delete("/{id}")
    @Operation(summary = "Удалить позицию")
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        inventoryItemMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
