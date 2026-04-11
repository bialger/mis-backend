package com.bialger.api

import com.bialger.api.dto.SystemSettingCreateDto
import com.bialger.api.dto.SystemSettingRestDto
import com.bialger.api.dto.SystemSettingUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.system.entity.SystemSettingEntity
import com.bialger.domain.system.mvc.SystemSettingMvcService
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

@Controller("/api/system-settings")
@Tag(name = "SystemSettings", description = "System settings (roles and integrations within the MIS scope)")
open class SystemSettingsApiController(
    private val systemSettingMvcService: SystemSettingMvcService
) {

    private fun toDto(s: SystemSettingEntity): SystemSettingRestDto = SystemSettingRestDto(
        id = s.id.toString(),
        branchId = s.branchId?.toString() ?: "",
        key = s.key,
        value = s.value ?: "",
        description = s.description ?: ""
    )

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List settings (paginated)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when adjacent pages exist",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = SystemSettingRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid page/size parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<SystemSettingRestDto>> {
        val all = systemSettingMvcService.listAll().map { toDto(it) }
        val page = ApiPage.slice(all, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get setting by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Found setting",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = SystemSettingRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): SystemSettingRestDto {
        val s = systemSettingMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return toDto(s)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create setting")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created record",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = SystemSettingRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Request body validation error")
    )
    open fun create(@Body @Valid dto: SystemSettingCreateDto): SystemSettingRestDto {
        val e = systemSettingMvcService.create(
            branchId = dto.branchId,
            key = dto.key,
            value = dto.value,
            description = dto.description
        )
        return toDto(e)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update setting")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated record",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = SystemSettingRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation error, duplicate key, or record not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: SystemSettingUpdateDto): SystemSettingRestDto {
        val e = systemSettingMvcService.update(
            id = id,
            branchId = dto.branchId,
            key = dto.key,
            value = dto.value,
            description = dto.description
        )
        return toDto(e)
    }

    @Delete("/{id}")
    @Operation(summary = "Delete setting")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Record not found")
    )
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        systemSettingMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
