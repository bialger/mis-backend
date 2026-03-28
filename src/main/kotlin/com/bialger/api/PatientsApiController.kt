package com.bialger.api

import com.bialger.application.shell.CrmShellApplicationService
import com.bialger.api.dto.PatientCreateDto
import com.bialger.api.dto.PatientRestDto
import com.bialger.api.dto.PatientUpdateDto
import com.bialger.api.http.PaginationLinks
import com.bialger.api.util.ApiPage
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.mvc.PatientMvcService
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
import io.micronaut.http.exceptions.HttpStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/patients")
@Tag(name = "Patients", description = "Пациенты")
open class PatientsApiController(
    private val patientMvcService: PatientMvcService,
    private val crmShellApplicationService: CrmShellApplicationService
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Список пациентов с пагинацией")
    @ApiResponse(responseCode = "200", description = "Страница")
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
    @Operation(summary = "Пациент по id")
    fun getOne(@PathVariable id: UUID): PatientRestDto {
        val p = patientMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return toDto(p)
    }

    @Get("/{id}/appointments", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Записи пациента")
    fun appointments(@PathVariable id: UUID): List<Map<String, Any?>> {
        patientMvcService.getById(id) ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Not found")
        return crmShellApplicationService.appointmentMapsForPatient(id)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Создать пациента")
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
    @Operation(summary = "Обновить пациента")
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
    @Operation(summary = "Удалить пациента")
    @ApiResponse(responseCode = "204", description = "Удалено")
    fun delete(@PathVariable id: UUID): HttpResponse<*> {
        patientMvcService.delete(id)
        return HttpResponse.noContent<Any>()
    }

    private fun toDto(e: PatientEntity): PatientRestDto = PatientRestDto(
        id = e.id.toString(),
        organizationId = e.organizationId.toString(),
        cardNumber = e.cardNumber,
        fullName = e.fullName,
        gender = e.gender?.name,
        birthDate = e.birthDate?.toString(),
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
        workplace = e.workplace
    )
}
