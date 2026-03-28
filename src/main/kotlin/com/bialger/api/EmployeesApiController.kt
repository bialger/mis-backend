package com.bialger.api

import com.bialger.api.dto.EmployeeCreateDto
import com.bialger.api.dto.EmployeeUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.mvc.EmployeeMvcService
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

@Controller("/api/employees")
@Tag(name = "Employees", description = "Employees (staff accounts)")
open class EmployeesApiController(
    private val employeeMvcService: EmployeeMvcService
) {

    private fun toMap(e: EmployeeEntity): Map<String, Any?> {
        val x = employeeMvcService.formExtras(e.id)
        @Suppress("UNCHECKED_CAST")
        val specIds = (x["employeeSpecialtyIds"] as? Set<String>)?.toList() ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val branchIds = (x["employeeBranchIds"] as? Set<String>)?.toList() ?: emptyList()
        val roleId = (x["employeeRoleId"] as? String)?.trim().orEmpty()
        return mapOf(
            "id" to e.id.toString(),
            "fullName" to e.fullName,
            "email" to e.email,
            "phone" to e.phone,
            "isActive" to e.isActive,
            "login" to (e.email ?: e.id.toString().take(8)),
            "name" to e.fullName,
            "role" to roleId,
            "roleId" to roleId,
            "specialtyIds" to specIds,
            "branchIds" to branchIds,
            "branchScope" to branchIds
        )
    }

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List employees (paginated)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when adjacent pages exist"),
        ApiResponse(responseCode = "400", description = "Invalid page/size parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<Map<String, Any?>>> {
        val rows = employeeMvcService.listAll().map { toMap(it) }
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get employee by id")
    fun getOne(@PathVariable id: UUID): Map<String, Any?> {
        val e = employeeMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return toMap(e)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create employee")
    open fun create(@Body @Valid dto: EmployeeCreateDto): Map<String, Any?> {
        val e = employeeMvcService.create(
            fullName = dto.fullName,
            email = dto.email,
            phone = dto.phone,
            passwordPlain = dto.password,
            isActive = dto.isActive,
            specialtyIds = dto.specialtyIds,
            branchIds = dto.branchIds,
            roleId = dto.roleId
        )
        return toMap(e)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update employee")
    open fun update(@PathVariable id: UUID, @Body @Valid dto: EmployeeUpdateDto): Map<String, Any?> {
        val e = employeeMvcService.update(
            id = id,
            fullName = dto.fullName,
            email = dto.email,
            phone = dto.phone,
            passwordPlain = dto.password,
            isActive = dto.isActive,
            specialtyIds = dto.specialtyIds,
            branchIds = dto.branchIds,
            roleId = dto.roleId
        )
        return toMap(e)
    }

    @Delete("/{id}")
    @Operation(summary = "Delete employee")
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        employeeMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
