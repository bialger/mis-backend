package com.bialger.domain.patient.mvc

import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.mvc.PatientMvcForm
import com.bialger.domain.mvc.parseUuidOrNull
import com.bialger.web.AppPageModelFactory
import com.bialger.web.DomainEventSseHub
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.sse.Event
import io.micronaut.views.ModelAndView
import org.reactivestreams.Publisher
import java.net.URI
import java.util.UUID

@Controller("/mvc/patients")
class PatientMvcController(
    private val patientMvcService: PatientMvcService,
    private val organizationRepository: OrganizationRepository,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(PatientMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Пациенты (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/patients/list",
            extra = mapOf(
                "patients" to patientMvcService.listAll(),
                "sseEventsPath" to "/mvc/patients/events",
                "sseEventName" to PatientMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый пациент",
            activePage = "mvc",
            contentView = "pages/content/mvc/patients/form-add",
            extra = mapOf("organizations" to organizationRepository.findAllOrdered())
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: PatientMvcForm): HttpResponse<Any> {
        return try {
            val org = form.organizationId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите организацию")
            val e = patientMvcService.create(
                org,
                form.cardNumber,
                form.fullName,
                PatientMvcService.parseGender(form.gender),
                PatientMvcService.parseBirthDate(form.birthDate),
                form.phone,
                form.email
            )
            HttpResponse.seeOther(URI.create("/mvc/patients/${e.id}"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый пациент",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/patients/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "organizations" to organizationRepository.findAllOrdered()
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val patient = patientMvcService.getById(id) ?: return HttpResponse.notFound()
        val orgName = organizationRepository.findById(patient.organizationId)
            .map { it.name }
            .orElse(patient.organizationId.toString())
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = patient.fullName,
                activePage = "mvc",
                contentView = "pages/content/mvc/patients/detail",
                extra = mapOf(
                    "patient" to patient,
                    "organizationName" to orgName
                )
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val patient = patientMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${patient.fullName}",
                activePage = "mvc",
                contentView = "pages/content/mvc/patients/form-edit",
                extra = mapOf(
                    "patient" to patient,
                    "organizations" to organizationRepository.findAllOrdered()
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: PatientMvcForm): HttpResponse<Any> {
        return try {
            val org = form.organizationId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите организацию")
            patientMvcService.update(
                id,
                org,
                form.cardNumber,
                form.fullName,
                PatientMvcService.parseGender(form.gender),
                PatientMvcService.parseBirthDate(form.birthDate),
                form.phone,
                form.email
            )
            HttpResponse.seeOther(URI.create("/mvc/patients/$id"))
        } catch (e: Exception) {
            val patient = patientMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${patient.fullName}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/patients/form-edit",
                    extra = mapOf(
                        "patient" to patient,
                        "organizations" to organizationRepository.findAllOrdered(),
                        "error" to (e.message ?: "Ошибка")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            patientMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/patients"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
