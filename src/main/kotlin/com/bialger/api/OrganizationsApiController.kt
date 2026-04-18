package com.bialger.api

import com.bialger.api.dto.OrganizationCreateDto
import com.bialger.api.dto.OrganizationRestDto
import com.bialger.api.dto.OrganizationUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.mvc.OrganizationMvcService
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

@Controller("/api/organizations")
@Tag(name = "Organizations", description = "Organizations")
open class OrganizationsApiController(
    private val organizationMvcService: OrganizationMvcService
) {

    private fun toDto(o: OrganizationEntity): OrganizationRestDto = OrganizationRestDto(
        id = o.id.toString(),
        name = o.name,
        codeOkpo = o.codeOkpo,
        codeOkud = o.codeOkud,
        address = o.address
    )

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List organizations (paginated)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when adjacent pages exist",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = OrganizationRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid page/size parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<OrganizationRestDto>> {
        val rows = organizationMvcService.listAll().map(::toDto)
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get organization by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Organization"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): OrganizationRestDto =
        organizationMvcService.getById(id)?.let(::toDto)
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create organization")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created organization"),
        ApiResponse(responseCode = "400", description = "Validation or business error")
    )
    open fun create(@Body @Valid dto: OrganizationCreateDto): OrganizationRestDto =
        toDto(
            organizationMvcService.create(
                name = dto.name,
                codeOkpo = dto.codeOkpo,
                codeOkud = dto.codeOkud,
                address = dto.address
            )
        )

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update organization")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated organization"),
        ApiResponse(responseCode = "400", description = "Validation or business error"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: OrganizationUpdateDto): OrganizationRestDto =
        toDto(
            organizationMvcService.update(
                id = id,
                name = dto.name,
                codeOkpo = dto.codeOkpo,
                codeOkud = dto.codeOkud,
                address = dto.address
            )
        )

    @Delete("/{id}")
    @Operation(summary = "Delete organization")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Not found or cannot delete")
    )
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        organizationMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
