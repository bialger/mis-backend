package com.bialger.api

import com.bialger.api.dto.BranchCreateDto
import com.bialger.api.dto.BranchUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.core.mvc.BranchListRow
import com.bialger.domain.core.mvc.BranchMvcService
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
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/branches")
@Tag(name = "Branches", description = "Branches (multi-clinic network)")
open class BranchesApiController(
    private val branchMvcService: BranchMvcService
) {

    private fun rowToMap(row: BranchListRow): Map<String, Any?> {
        val b = row.branch
        return mapOf(
            "id" to b.id.toString(),
            "organizationId" to b.organizationId.toString(),
            "organizationName" to row.organizationName,
            "name" to b.name,
            "address" to b.address,
            "phone" to b.phone,
            "isActive" to b.isActive
        )
    }

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List branches (paginated)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when adjacent pages exist"),
        ApiResponse(responseCode = "400", description = "Invalid page/size parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<Map<String, Any?>>> {
        val rows = branchMvcService.listRows().map { rowToMap(it) }
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get branch by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Branch"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): Map<String, Any?> {
        val row = branchMvcService.listRows().find { it.branch.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return rowToMap(row)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create branch")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created"),
        ApiResponse(responseCode = "400", description = "Validation error")
    )
    open fun create(@Body @Valid dto: BranchCreateDto): Map<String, Any?> {
        val e = branchMvcService.create(
            organizationId = dto.organizationId,
            name = dto.name,
            address = dto.address,
            phone = dto.phone,
            isActive = dto.isActive
        )
        val row = branchMvcService.listRows().find { it.branch.id == e.id }
            ?: throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load branch")
        return rowToMap(row)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update branch")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated"),
        ApiResponse(responseCode = "400", description = "Validation error"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: BranchUpdateDto): Map<String, Any?> {
        branchMvcService.update(
            id = id,
            organizationId = dto.organizationId,
            name = dto.name,
            address = dto.address,
            phone = dto.phone,
            isActive = dto.isActive
        )
        val row = branchMvcService.listRows().find { it.branch.id == id }
            ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return rowToMap(row)
    }

    @Delete("/{id}")
    @Operation(summary = "Delete branch")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Not found or cannot delete")
    )
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        branchMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
