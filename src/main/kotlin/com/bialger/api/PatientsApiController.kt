package com.bialger.api

import com.bialger.api.dto.AppointmentRestDto
import com.bialger.api.dto.PatientCreateDto
import com.bialger.api.dto.PatientRestDto
import com.bialger.api.dto.PatientTagsReplaceDto
import com.bialger.api.dto.PatientTagsResponseDto
import com.bialger.api.dto.PatientUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.mvc.PatientMvcService
import com.bialger.domain.patient.mvc.PatientTagMvcService
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.mvc.AppointmentListRow
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import com.bialger.domain.scheduling.repository.TimeSlotRepository
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
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Controller("/api/patients")
@Tag(name = "Patients", description = "Patients (card and entity fields as supported by the domain model)")
open class PatientsApiController(
    private val patientMvcService: PatientMvcService,
    private val patientTagMvcService: PatientTagMvcService,
    private val appointmentMvcService: AppointmentMvcService,
    private val timeSlotRepository: TimeSlotRepository
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "List patients (paginated)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Page; Link header when multiple pages exist"),
        ApiResponse(responseCode = "400", description = "Invalid pagination")
    )
    fun list(
        pageable: Pageable,
        request: HttpRequest<*>
    ): HttpResponse<Page<PatientRestDto>> {
        val all = patientMvcService.listAll().map { toDto(it) }
        val page = ApiPage.slice(all, pageable)
        val resp = HttpResponse.ok(page)
        PaginationLinks.appendToResponse(request, page, resp)
        return resp
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get patient by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Patient"),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): PatientRestDto {
        val p = patientMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return toDto(p)
    }

    @Get("/{id}/appointments", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Patient appointments (nested collection under the patient)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of appointments"),
        ApiResponse(responseCode = "404", description = "Patient not found")
    )
    fun appointments(@PathVariable id: UUID): List<AppointmentRestDto> {
        patientMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        return appointmentMvcService.listRows()
            .filter { it.appointment.patientId == id }
            .map { appointmentRowToDto(it, slots) }
    }

    @Get("/{id}/tags", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Assigned patient tag types (patient_tag rows)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Tag type ids assigned to the patient",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PatientTagsResponseDto::class))]),
        ApiResponse(responseCode = "404", description = "Patient not found")
    )
    fun listTags(@PathVariable id: UUID): PatientTagsResponseDto {
        patientMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return PatientTagsResponseDto(
            tagTypeIds = patientTagMvcService.listTagTypeIds(id).map { it.toString() }
        )
    }

    @Put("/{id}/tags", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Replace all patient tags (full set of tag type ids)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Saved tag type ids",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PatientTagsResponseDto::class))]),
        ApiResponse(responseCode = "400", description = "Unknown or inactive tag type"),
        ApiResponse(responseCode = "404", description = "Patient not found")
    )
    open fun replaceTags(@PathVariable id: UUID, @Body @Valid dto: PatientTagsReplaceDto): PatientTagsResponseDto {
        patientMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        try {
            patientTagMvcService.replaceTags(id, dto.tagTypeIds)
        } catch (e: IllegalArgumentException) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, e.message ?: "bad request")
        }
        return PatientTagsResponseDto(tagTypeIds = dto.tagTypeIds.map { it.toString() })
    }

    private fun appointmentRowToDto(row: AppointmentListRow, slots: Map<UUID, TimeSlotEntity>): AppointmentRestDto {
        val a = row.appointment
        val start = resolveStart(a, slots)
        val end = resolveEnd(a, slots)
        return AppointmentRestDto(
            id = a.id.toString(),
            patientId = a.patientId.toString(),
            patientName = row.patientName,
            employeeName = row.employeeName,
            slotLabel = row.slotLabel,
            doctorId = a.employeeId.toString(),
            employeeId = a.employeeId.toString(),
            branchId = a.branchId.toString(),
            roomId = a.roomId.toString(),
            status = mapStatus(a.status),
            statusApi = a.status.name,
            source = a.source.name,
            start = start?.toString(),
            end = end?.toString(),
            notes = a.notes,
            timeSlotId = a.timeSlotId?.toString()
        )
    }

    private fun resolveStart(a: AppointmentEntity, slots: Map<UUID, TimeSlotEntity>): Instant? {
        val t = a.timeSlotId?.let { slots[it] } ?: return a.createdAt
        return t.slotDate.atTime(t.startTime).atZone(ZoneId.systemDefault()).toInstant()
    }

    private fun resolveEnd(a: AppointmentEntity, slots: Map<UUID, TimeSlotEntity>): Instant? {
        val t = a.timeSlotId?.let { slots[it] }
        return if (t != null) {
            t.slotDate.atTime(t.endTime).atZone(ZoneId.systemDefault()).toInstant()
        } else {
            val s = a.createdAt ?: return null
            Instant.ofEpochMilli(s.toEpochMilli() + 30 * 60_000L)
        }
    }

    private fun mapStatus(s: AppointmentStatus): String = when (s) {
        AppointmentStatus.SCHEDULED -> "BOOKED"
        AppointmentStatus.CONFIRMED -> "CONFIRMED"
        AppointmentStatus.ARRIVED -> "CONFIRMED"
        AppointmentStatus.NO_SHOW -> "CANCELLED"
        AppointmentStatus.CANCELLED -> "CANCELLED"
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Create patient")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created patient"),
        ApiResponse(responseCode = "400", description = "Validation or field parsing error")
    )
    open fun create(@Body @Valid dto: PatientCreateDto): PatientRestDto {
        val e = patientMvcService.create(
            organizationId = dto.organizationId,
            cardNumber = dto.cardNumber,
            fullName = dto.fullName,
            gender = dto.gender?.let { PatientMvcService.parseGender(it) },
            birthDate = dto.birthDate?.let { PatientMvcService.parseBirthDate(it) },
            phone = dto.phone,
            email = dto.email,
            registrationAddress = dto.registrationAddress,
            residenceAddress = dto.residenceAddress,
            localityType = dto.localityType?.let { PatientMvcService.parseLocalityType(it) },
            citizenship = dto.citizenship,
            identityDocument = dto.identityDocument,
            omsPolicy = dto.omsPolicy,
            snils = dto.snils,
            insuranceOrganization = dto.insuranceOrganization,
            contactPerson = dto.contactPerson,
            guardian = dto.guardian,
            profession = dto.profession,
            workplace = dto.workplace
        )
        return toDto(e)
    }

    @Patch("/{id}", processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Update patient")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Updated patient"),
        ApiResponse(responseCode = "400", description = "Validation or business error")
    )
    open fun update(@PathVariable id: UUID, @Body @Valid dto: PatientUpdateDto): PatientRestDto {
        val e = patientMvcService.update(
            id = id,
            organizationId = dto.organizationId,
            cardNumber = dto.cardNumber,
            fullName = dto.fullName,
            gender = dto.gender?.let { PatientMvcService.parseGender(it) },
            birthDate = dto.birthDate?.let { PatientMvcService.parseBirthDate(it) },
            phone = dto.phone,
            email = dto.email,
            registrationAddress = dto.registrationAddress,
            residenceAddress = dto.residenceAddress,
            localityType = dto.localityType?.let { PatientMvcService.parseLocalityType(it) },
            citizenship = dto.citizenship,
            identityDocument = dto.identityDocument,
            omsPolicy = dto.omsPolicy,
            snils = dto.snils,
            insuranceOrganization = dto.insuranceOrganization,
            contactPerson = dto.contactPerson,
            guardian = dto.guardian,
            profession = dto.profession,
            workplace = dto.workplace
        )
        return toDto(e)
    }

    @Delete("/{id}")
    @Operation(summary = "Delete patient")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Deleted"),
        ApiResponse(responseCode = "400", description = "Cannot delete or not found")
    )
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        patientMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }

    private fun toDto(e: PatientEntity): PatientRestDto {
        val bd = e.birthDate?.toString()
        return PatientRestDto(
            id = e.id.toString(),
            organizationId = e.organizationId.toString(),
            cardNumber = e.cardNumber,
            fullName = e.fullName,
            gender = e.gender?.name,
            birthDate = bd,
            dob = bd,
            phone = e.phone,
            email = e.email,
            registrationAddress = e.registrationAddress,
            residenceAddress = e.residenceAddress,
            localityType = e.localityType?.name,
            citizenship = e.citizenship,
            identityDocument = e.identityDocument,
            omsPolicy = e.omsPolicy,
            snils = e.snils,
            insuranceOrganization = e.insuranceOrganization,
            contactPerson = e.contactPerson,
            guardian = e.guardian,
            profession = e.profession,
            workplace = e.workplace,
            icons = emptyList()
        )
    }
}
