package com.bialger.api

import com.bialger.domain.attachment.repository.IntegrationRepository
import com.bialger.domain.clinical.repository.TemplateRepository
import com.bialger.domain.patient.repository.PatientTagTypeRepository
import com.bialger.domain.scheduling.repository.ServiceRepository
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * Read-only lists for settings / shell (same data as MVC catalogs).
 */
@Controller("/api/catalog")
@Tag(name = "Catalog", description = "Reference data: services, templates, integrations, patient tag types")
open class CatalogApiController(
    private val serviceRepository: ServiceRepository,
    private val templateRepository: TemplateRepository,
    private val integrationRepository: IntegrationRepository,
    private val patientTagTypeRepository: PatientTagTypeRepository
) {

    @Get("/services", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Medical services (price list)")
    fun services(): List<Map<String, Any?>> =
        serviceRepository.findAllOrdered().map { s ->
            mapOf(
                "id" to s.id.toString(),
                "name" to s.name,
                "price" to s.price.toPlainString(),
                "costPrice" to s.costPrice?.toPlainString(),
                "branchId" to s.branchId?.toString(),
                "isActive" to s.isActive
            )
        }

    @Get("/templates", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Document templates")
    fun templates(): List<Map<String, Any?>> =
        templateRepository.findAllOrdered().map { t ->
            mapOf(
                "id" to t.id.toString(),
                "name" to t.name,
                "type" to t.type.name,
                "specialtyId" to t.specialtyId?.toString(),
                "employeeId" to t.employeeId?.toString(),
                "isActive" to t.isActive,
                "contentPreview" to t.content.take(200)
            )
        }

    @Get("/integrations", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "External integrations")
    fun integrations(): List<Map<String, Any?>> =
        integrationRepository.findAllOrdered().map { i ->
            mapOf(
                "id" to i.id.toString(),
                "name" to i.name,
                "type" to i.type.name,
                "isActive" to i.isActive,
                "config" to (i.config ?: "")
            )
        }

    @Get("/patient-tag-types", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Patient tag / icon types")
    fun patientTagTypes(): List<Map<String, Any?>> =
        patientTagTypeRepository.findAllOrdered().map { p ->
            mapOf(
                "id" to p.id.toString(),
                "code" to p.code,
                "name" to p.name,
                "icon" to (p.icon ?: ""),
                "description" to (p.description ?: ""),
                "isActive" to p.isActive
            )
        }
}
