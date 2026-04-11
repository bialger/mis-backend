package com.bialger.api

import com.bialger.api.dto.PatientConsentRestDto
import com.bialger.domain.patient.entity.PatientConsentEntity
import com.bialger.domain.patient.enums.ConsentType
import com.bialger.domain.patient.repository.PatientConsentRepository
import com.bialger.domain.patient.repository.PatientRepository
import io.micronaut.core.annotation.Introspected
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
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Instant
import java.util.UUID

@Serdeable
@Introspected
data class PatientConsentDto(
    val consentType: String,
    val isGranted: Boolean
)

@Controller("/api/patients/{patientId}/consents")
@Tag(name = "PatientConsents", description = "Patient consents (GOV_DATA_TRANSFER, MARKETING, etc.)")
open class PatientConsentsApiController(
    private val patientConsentRepository: PatientConsentRepository,
    private val patientRepository: PatientRepository
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List consents for a patient")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of consents (may be empty)",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PatientConsentRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Patient not found")
    )
    fun list(@PathVariable patientId: UUID): List<PatientConsentRestDto> {
        requirePatientExists(patientId)
        return patientConsentRepository.findByPatientId(patientId).map { toDto(it) }
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Grant or record a patient consent")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created or updated consent",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PatientConsentRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Unknown consent type"),
        ApiResponse(responseCode = "404", description = "Patient not found")
    )
    open fun create(@PathVariable patientId: UUID, @Body dto: PatientConsentDto): PatientConsentRestDto {
        requirePatientExists(patientId)
        val type = parseConsentType(dto.consentType)
        val existing = patientConsentRepository.findByPatientId(patientId).find { it.consentType == type }
        if (existing != null) {
            val updated = existing.copy(
                isGranted = dto.isGranted,
                grantedAt = if (dto.isGranted) Instant.now() else existing.grantedAt,
                revokedAt = if (!dto.isGranted) Instant.now() else null
            )
            patientConsentRepository.update(updated)
            return toDto(updated)
        }
        val entity = PatientConsentEntity(
            id = UUID.randomUUID(),
            patientId = patientId,
            consentType = type,
            isGranted = dto.isGranted,
            grantedAt = if (dto.isGranted) Instant.now() else null,
            revokedAt = if (!dto.isGranted) Instant.now() else null
        )
        patientConsentRepository.save(entity)
        return toDto(entity)
    }

    @Patch("/{consentId}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update (grant or revoke) a specific consent")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated consent",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PatientConsentRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Patient or consent not found")
    )
    open fun update(
        @PathVariable patientId: UUID,
        @PathVariable consentId: UUID,
        @Body dto: PatientConsentDto
    ): PatientConsentRestDto {
        requirePatientExists(patientId)
        val existing = patientConsentRepository.findById(consentId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Consent not found") }
        if (existing.patientId != patientId) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Consent does not belong to this patient")
        }
        val updated = existing.copy(
            isGranted = dto.isGranted,
            grantedAt = if (dto.isGranted) Instant.now() else existing.grantedAt,
            revokedAt = if (!dto.isGranted) Instant.now() else null
        )
        patientConsentRepository.update(updated)
        return toDto(updated)
    }

    @Delete("/{consentId}")
    @Operation(summary = "Delete a patient consent record")
    open fun delete(@PathVariable patientId: UUID, @PathVariable consentId: UUID): HttpResponse<*> {
        requirePatientExists(patientId)
        val existing = patientConsentRepository.findById(consentId)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Consent not found") }
        if (existing.patientId != patientId) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Consent does not belong to this patient")
        }
        patientConsentRepository.deleteById(consentId)
        return HttpResponse.noContent<Any>()
    }

    private fun requirePatientExists(patientId: UUID) {
        if (!patientRepository.findById(patientId).isPresent) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "Patient not found")
        }
    }

    private fun parseConsentType(raw: String): ConsentType =
        runCatching { ConsentType.valueOf(raw.trim().uppercase()) }
            .getOrElse { throw HttpStatusException(HttpStatus.BAD_REQUEST, "Unknown consent type: $raw") }

    private fun toDto(e: PatientConsentEntity): PatientConsentRestDto = PatientConsentRestDto(
        id = e.id.toString(),
        patientId = e.patientId.toString(),
        consentType = e.consentType.name,
        isGranted = e.isGranted,
        grantedAt = e.grantedAt?.toString(),
        revokedAt = e.revokedAt?.toString()
    )
}
