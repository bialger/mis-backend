package com.bialger.api

import com.bialger.api.dto.SystemSettingCreateDto
import com.bialger.api.dto.SystemSettingUpdateDto
import com.bialger.domain.system.mvc.SystemSettingMvcService
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

@Controller("/api/system-settings")
@Tag(name = "SystemSettings", description = "Системные настройки")
open class SystemSettingsApiController(
    private val systemSettingMvcService: SystemSettingMvcService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Список настроек")
    fun list(): List<Map<String, Any?>> =
        systemSettingMvcService.listAll().map { s ->
            mapOf(
                "id" to s.id.toString(),
                "branchId" to (s.branchId?.toString() ?: ""),
                "key" to s.key,
                "value" to (s.value ?: ""),
                "description" to (s.description ?: "")
            )
        }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Настройка по id")
    fun getOne(@PathVariable id: UUID): Map<String, Any?> {
        val s = systemSettingMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return mapOf(
            "id" to s.id.toString(),
            "branchId" to (s.branchId?.toString() ?: ""),
            "key" to s.key,
            "value" to (s.value ?: ""),
            "description" to (s.description ?: "")
        )
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Создать настройку")
    open fun create(@Body @Valid dto: SystemSettingCreateDto): Map<String, Any?> {
        val e = systemSettingMvcService.create(
            branchId = dto.branchId,
            key = dto.key,
            value = dto.value,
            description = dto.description
        )
        return mapOf(
            "id" to e.id.toString(),
            "branchId" to (e.branchId?.toString() ?: ""),
            "key" to e.key,
            "value" to (e.value ?: ""),
            "description" to (e.description ?: "")
        )
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Обновить настройку")
    open fun update(@PathVariable id: UUID, @Body @Valid dto: SystemSettingUpdateDto): Map<String, Any?> {
        val e = systemSettingMvcService.update(
            id = id,
            branchId = dto.branchId,
            key = dto.key,
            value = dto.value,
            description = dto.description
        )
        return mapOf(
            "id" to e.id.toString(),
            "branchId" to (e.branchId?.toString() ?: ""),
            "key" to e.key,
            "value" to (e.value ?: ""),
            "description" to (e.description ?: "")
        )
    }

    @Delete("/{id}")
    @Operation(summary = "Удалить настройку")
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        systemSettingMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }
}
