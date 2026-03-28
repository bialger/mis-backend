package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Controller("/api/employees")
@Tag(name = "Employees", description = "Сотрудники")
class EmployeesApiController(
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Список сотрудников (пользователей CRM)")
    fun list(): List<Map<String, Any?>> = crmShellApplicationService.employeesList()
}
