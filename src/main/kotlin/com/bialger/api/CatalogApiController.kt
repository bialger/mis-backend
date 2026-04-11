package com.bialger.api

import com.bialger.api.dto.CatalogIntegrationRestDto
import com.bialger.api.dto.CatalogServiceRestDto
import com.bialger.api.dto.CatalogTemplateRestDto
import com.bialger.api.dto.PatientTagTypeRestDto
import com.bialger.domain.attachment.repository.IntegrationRepository
import com.bialger.domain.clinical.repository.TemplateRepository
import com.bialger.domain.patient.repository.PatientTagTypeRepository
import com.bialger.domain.scheduling.repository.ServiceRepository
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of services",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CatalogServiceRestDto::class))])
    )
    fun services(): List<CatalogServiceRestDto> =
        serviceRepository.findAllOrdered().map { s ->
            CatalogServiceRestDto(
                id = s.id.toString(),
                name = s.name,
                price = s.price.toPlainString(),
                costPrice = s.costPrice?.toPlainString(),
                branchId = s.branchId?.toString(),
                isActive = s.isActive
            )
        }

    @Get("/templates", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Document templates")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of templates",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CatalogTemplateRestDto::class))])
    )
    fun templates(): List<CatalogTemplateRestDto> =
        templateRepository.findAllOrdered().map { t ->
            CatalogTemplateRestDto(
                id = t.id.toString(),
                name = t.name,
                type = t.type.name,
                specialtyId = t.specialtyId?.toString(),
                employeeId = t.employeeId?.toString(),
                isActive = t.isActive,
                contentPreview = t.content.take(200)
            )
        }

    @Get("/integrations", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "External integrations")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of integrations",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CatalogIntegrationRestDto::class))])
    )
    fun integrations(): List<CatalogIntegrationRestDto> =
        integrationRepository.findAllOrdered().map { i ->
            CatalogIntegrationRestDto(
                id = i.id.toString(),
                name = i.name,
                type = i.type.name,
                isActive = i.isActive,
                config = i.config ?: ""
            )
        }

    @Get("/patient-tag-types", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Patient tag / icon types")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of tag types",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PatientTagTypeRestDto::class))])
    )
    fun patientTagTypes(): List<PatientTagTypeRestDto> =
        patientTagTypeRepository.findAllOrdered().map { p ->
            PatientTagTypeRestDto(
                id = p.id.toString(),
                code = p.code,
                name = p.name,
                icon = p.icon ?: "",
                description = p.description ?: "",
                isActive = p.isActive
            )
        }
}
