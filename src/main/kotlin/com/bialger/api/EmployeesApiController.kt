package com.bialger.api

import com.bialger.api.dto.EmployeeCreateDto
import com.bialger.api.dto.EmployeeRestDto
import com.bialger.api.dto.EmployeeUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.mvc.EmployeeMvcService
import com.bialger.domain.core.repository.RoleRepository
import java.util.UUID as JavaUUID
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

@Controller("/api/employees")
@Tag(name = "Employees", description = "Employees (staff accounts)")
open class EmployeesApiController(
    private val employeeMvcService: EmployeeMvcService,
    private val roleRepository: RoleRepository
) {

    private fun toDto(e: EmployeeEntity): EmployeeRestDto {
        val x = employeeMvcService.formExtras(e.id)
        @Suppress("UNCHECKED_CAST")
        val specIds = (x["employeeSpecialtyIds"] as? Set<String>)?.toList() ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val branchIds = (x["employeeBranchIds"] as? Set<String>)?.toList() ?: emptyList()
        val roleId = (x["employeeRoleId"] as? String)?.trim().orEmpty()
        val roleEnt = runCatching { JavaUUID.fromString(roleId) }.getOrNull()
            ?.let { roleRepository.findById(it).orElse(null) }
        return EmployeeRestDto(
            id = e.id.toString(),
            fullName = e.fullName,
            email = e.email,
            phone = e.phone,
            isActive = e.isActive,
            login = e.email ?: e.id.toString().take(8),
            name = e.fullName,
            role = roleId,
            roleId = roleId,
            roleCode = roleEnt?.name ?: "",
            roleLabel = roleEnt?.displayName ?: roleEnt?.name ?: "",
            specialtyIds = specIds,
            branchIds = branchIds,
            branchScope = branchIds
        )
    }

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List employees (paginated)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page of employees; Link header when adjacent pages exist",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = EmployeeRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid page/size parameters")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<EmployeeRestDto>> {
        val rows = employeeMvcService.listAll().map { toDto(it) }
        val page = ApiPage.slice(rows, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get employee by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Employee",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = EmployeeRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): EmployeeRestDto {
        val e = employeeMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return toDto(e)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create employee")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created employee",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = EmployeeRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation or business error")
    )
    open fun create(@Body @Valid dto: EmployeeCreateDto): EmployeeRestDto {
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
        return toDto(e)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update employee")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated employee",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = EmployeeRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation or business error"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: EmployeeUpdateDto): EmployeeRestDto {
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
        return toDto(e)
    }

    @Delete("/{id}")
    @Operation(summary = "Delete employee")
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        employeeMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
