package com.bialger.api

import com.bialger.api.dto.MedicalRecordUpdateDto
import com.bialger.domain.clinical.mvc.MedicalRecordMvcService
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
    @ApiResponse(responseCode = "200", description = "List (empty if none)")
    fun list(@QueryValue patientId: UUID): List<Map<String, Any?>> =
        medicalRecordMvcService.listMapsByPatientId(patientId)

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get medical record by id")
    fun getOne(@PathVariable id: UUID): Map<String, Any?> {
        val e = medicalRecordMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return medicalRecordMvcService.toMap(e)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update medical record text fields")
    open fun update(@PathVariable id: UUID, @Body @Valid dto: MedicalRecordUpdateDto): Map<String, Any?> {
        val e = medicalRecordMvcService.update(
            id = id,
            complaints = dto.complaints,
            anamnesis = dto.anamnesis,
            examinationResults = dto.examinationResults,
            diseaseCourse = dto.diseaseCourse,
            procedures = dto.procedures,
            epicrisis = dto.epicrisis
        )
        return medicalRecordMvcService.toMap(e)
    }

    @Post("/ensure/{appointmentId}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Ensure a medical record exists for an appointment (creates if missing)")
    open fun ensureForAppointment(@PathVariable appointmentId: UUID): Map<String, Any?> {
        val e = medicalRecordMvcService.ensureForAppointment(appointmentId)
        return medicalRecordMvcService.toMap(e)
    }
}
