package com.bialger.api

import com.bialger.api.dto.InventoryItemCreateDto
import com.bialger.api.dto.InventoryItemRestDto
import com.bialger.api.dto.InventoryItemUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.inventory.mvc.InventoryItemListRow
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
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/inventory-items")
@Tag(name = "Inventory", description = "Warehouse and inventory items")
open class InventoryItemsApiController(
    private val inventoryItemMvcService: InventoryItemMvcService
) {

    private fun rowToDto(row: InventoryItemListRow): InventoryItemRestDto = InventoryItemRestDto(
        id = row.item.id.toString(),
        name = row.item.name,
        branchId = row.item.branchId.toString(),
        quantity = row.item.quantity,
        unit = row.item.unit,
        minQuantity = row.item.minQuantity,
        categoryName = row.categoryName,
        roomName = row.roomName ?: ""
    )

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List items")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when multiple pages exist",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = InventoryItemRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid pagination")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<InventoryItemRestDto>> {
        val rows = inventoryItemMvcService.listRows().map { rowToDto(it) }
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get item by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Item",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = InventoryItemRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): InventoryItemRestDto {
        val row = inventoryItemMvcService.listRows().find { it.item.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return rowToDto(row)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create item")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created item",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = InventoryItemRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation or business error"),
        ApiResponse(responseCode = "500", description = "Could not build response")
    )
    open fun create(@Body @Valid dto: InventoryItemCreateDto): InventoryItemRestDto {
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
        val row = inventoryItemMvcService.listRows().find { it.item.id == e.id }
            ?: throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load item")
        return rowToDto(row)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update item")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated item",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = InventoryItemRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation or business error"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: InventoryItemUpdateDto): InventoryItemRestDto {
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
        return rowToDto(row)
    }

    @Delete("/{id}")
    @Operation(summary = "Delete item")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Not found or cannot delete")
    )
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        inventoryItemMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
