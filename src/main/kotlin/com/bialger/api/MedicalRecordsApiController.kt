package com.bialger.api

import com.bialger.api.dto.MedicalRecordRestDto
import com.bialger.api.dto.MedicalRecordUpdateDto
import com.bialger.domain.clinical.mvc.MedicalRecordMvcService
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/medical-records")
@Tag(name = "MedicalRecords", description = "Clinical visit records (documentation per appointment)")
open class MedicalRecordsApiController(
    private val medicalRecordMvcService: MedicalRecordMvcService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List medical records for a patient")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List (empty if none)",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = MedicalRecordRestDto::class))])
    )
    fun list(@QueryValue patientId: UUID): List<MedicalRecordRestDto> =
        medicalRecordMvcService.listByPatientId(patientId)

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get medical record by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Medical record",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = MedicalRecordRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): MedicalRecordRestDto {
        val e = medicalRecordMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return medicalRecordMvcService.toDto(e)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update medical record text fields")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated record",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = MedicalRecordRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation error")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: MedicalRecordUpdateDto): MedicalRecordRestDto {
        val e = medicalRecordMvcService.update(
            id = id,
            complaints = dto.complaints,
            anamnesis = dto.anamnesis,
            examinationResults = dto.examinationResults,
            diseaseCourse = dto.diseaseCourse,
            procedures = dto.procedures,
            epicrisis = dto.epicrisis
        )
        return medicalRecordMvcService.toDto(e)
    }

    @Post("/ensure/{appointmentId}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Ensure a medical record exists for an appointment (creates if missing)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Existing or newly created record",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = MedicalRecordRestDto::class))])
    )
    open fun ensureForAppointment(@PathVariable appointmentId: UUID): MedicalRecordRestDto {
        val e = medicalRecordMvcService.ensureForAppointment(appointmentId)
        return medicalRecordMvcService.toDto(e)
    }

    @Delete("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "Delete a medical record",
        description = "Permanently removes the medical record. The deletion is recorded in the audit log."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Deleted record snapshot",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = MedicalRecordRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Not found"),
        ApiResponse(responseCode = "403", description = "Forbidden — insufficient permissions")
    )
    open fun delete(@PathVariable id: UUID): MedicalRecordRestDto {
        return try {
            val e = medicalRecordMvcService.delete(id)
            medicalRecordMvcService.toDto(e)
        } catch (ex: IllegalArgumentException) {
            throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        }
    }
}
