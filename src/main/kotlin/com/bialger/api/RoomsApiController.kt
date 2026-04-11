package com.bialger.api

import com.bialger.api.dto.RoomCreateDto
import com.bialger.api.dto.RoomRestDto
import com.bialger.api.dto.RoomUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.core.mvc.RoomListRow
import com.bialger.domain.core.mvc.RoomMvcService
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

@Controller("/api/rooms")
@Tag(name = "Rooms", description = "Rooms (linked to a branch)")
open class RoomsApiController(
    private val roomMvcService: RoomMvcService
) {

    private fun rowToDto(row: RoomListRow): RoomRestDto {
        val r = row.room
        return RoomRestDto(
            id = r.id.toString(),
            branchId = r.branchId.toString(),
            name = r.name,
            description = r.description,
            isActive = r.isActive
        )
    }

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List rooms (paginated)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of rooms; Link header when adjacent pages exist",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = RoomRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid page/size parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<RoomRestDto>> {
        val rows = roomMvcService.listRows().map { rowToDto(it) }
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get room by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Room",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = RoomRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): RoomRestDto {
        val row = roomMvcService.listRows().find { it.room.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return rowToDto(row)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create room")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created room",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = RoomRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation error")
    )
    open fun create(@Body @Valid dto: RoomCreateDto): RoomRestDto {
        val e = roomMvcService.create(
            branchId = dto.branchId,
            name = dto.name,
            description = dto.description,
            isActive = dto.isActive
        )
        val row = roomMvcService.listRows().find { it.room.id == e.id }
            ?: throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load room")
        return rowToDto(row)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update room")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated room",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = RoomRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: RoomUpdateDto): RoomRestDto {
        roomMvcService.update(
            id = id,
            branchId = dto.branchId,
            name = dto.name,
            description = dto.description,
            isActive = dto.isActive
        )
        val row = roomMvcService.listRows().find { it.room.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return rowToDto(row)
    }

    @Delete("/{id}")
    @Operation(summary = "Delete room")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Not found or cannot delete")
    )
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        roomMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
