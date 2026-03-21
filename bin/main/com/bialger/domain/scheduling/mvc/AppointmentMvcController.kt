package com.bialger.domain.scheduling.mvc

import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.mvc.AppointmentMvcForm
import com.bialger.domain.mvc.employeeDropdown
import com.bialger.domain.mvc.parseUuidOrNull
import com.bialger.domain.mvc.patientDropdown
import com.bialger.domain.mvc.timeSlotDropdown
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.repository.TimeSlotRepository
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

@Controller("/mvc/appointments")
class AppointmentMvcController(
    private val appointmentMvcService: AppointmentMvcService,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(AppointmentMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Приёмы (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/appointments/list",
            extra = mapOf(
                "rows" to appointmentMvcService.listRows(),
                "sseEventsPath" to "/mvc/appointments/events",
                "sseEventName" to AppointmentMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая запись",
            activePage = "mvc",
            contentView = "pages/content/mvc/appointments/form-add",
            extra = mapOf(
                "patients" to patientDropdown(patientRepository, null),
                "employees" to employeeDropdown(employeeRepository, null),
                "timeSlots" to timeSlotDropdown(timeSlotRepository, null),
                "branches" to branchRepository.findAllOrdered(),
                "rooms" to roomRepository.findAllOrdered(),
                "statuses" to AppointmentStatus.entries,
                "sources" to AppointmentSource.entries
            )
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: AppointmentMvcForm): HttpResponse<Any> {
        return try {
            val patientId = form.patientId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите пациента")
            val employeeId = form.employeeId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите сотрудника")
            val timeSlotId = form.timeSlotId.parseUuidOrNull()
            val branchId = form.branchId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите филиал")
            val roomId = form.roomId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите кабинет")
            val createdBy = form.createdBy.parseUuidOrNull()
            val e = appointmentMvcService.create(
                patientId,
                employeeId,
                timeSlotId,
                branchId,
                roomId,
                AppointmentMvcService.parseStatus(form.status),
                AppointmentMvcService.parseSource(form.source),
                form.notes,
                createdBy
            )
            HttpResponse.seeOther(URI.create("/mvc/appointments"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая запись",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/appointments/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "patients" to patientDropdown(patientRepository, null),
                        "employees" to employeeDropdown(employeeRepository, null),
                        "timeSlots" to timeSlotDropdown(timeSlotRepository, null),
                        "branches" to branchRepository.findAllOrdered(),
                        "rooms" to roomRepository.findAllOrdered(),
                        "statuses" to AppointmentStatus.entries,
                        "sources" to AppointmentSource.entries
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val a = appointmentMvcService.getById(id) ?: return HttpResponse.notFound()
        val row = appointmentMvcService.listRows().find { it.appointment.id == id }
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Приём ${a.status}",
                activePage = "mvc",
                contentView = "pages/content/mvc/appointments/detail",
                extra = mapOf(
                    "appointment" to a,
                    "patientName" to (row?.patientName ?: ""),
                    "employeeName" to (row?.employeeName ?: ""),
                    "branchName" to (row?.branchName ?: ""),
                    "roomName" to (row?.roomName ?: ""),
                    "slotLabel" to (row?.slotLabel ?: "")
                )
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val a = appointmentMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование приёма",
                activePage = "mvc",
                contentView = "pages/content/mvc/appointments/form-edit",
                extra = mapOf(
                    "appointment" to a,
                    "patients" to patientDropdown(patientRepository, a.patientId),
                    "employees" to employeeDropdown(employeeRepository, a.employeeId),
                    "timeSlots" to timeSlotDropdown(timeSlotRepository, a.timeSlotId),
                    "branches" to branchRepository.findAllOrdered(),
                    "rooms" to roomRepository.findAllOrdered(),
                    "statuses" to AppointmentStatus.entries,
                    "sources" to AppointmentSource.entries
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: AppointmentMvcForm): HttpResponse<Any> {
        return try {
            val patientId = form.patientId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите пациента")
            val employeeId = form.employeeId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите сотрудника")
            val timeSlotId = form.timeSlotId.parseUuidOrNull()
            val branchId = form.branchId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите филиал")
            val roomId = form.roomId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите кабинет")
            val createdBy = form.createdBy.parseUuidOrNull()
            appointmentMvcService.update(
                id,
                patientId,
                employeeId,
                timeSlotId,
                branchId,
                roomId,
                AppointmentMvcService.parseStatus(form.status),
                AppointmentMvcService.parseSource(form.source),
                form.notes,
                createdBy
            )
            HttpResponse.seeOther(URI.create("/mvc/appointments"))
        } catch (e: Exception) {
            val a = appointmentMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование приёма",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/appointments/form-edit",
                    extra = mapOf(
                        "appointment" to a,
                        "patients" to patientDropdown(patientRepository, a.patientId),
                        "employees" to employeeDropdown(employeeRepository, a.employeeId),
                        "timeSlots" to timeSlotDropdown(timeSlotRepository, a.timeSlotId),
                        "branches" to branchRepository.findAllOrdered(),
                        "rooms" to roomRepository.findAllOrdered(),
                        "statuses" to AppointmentStatus.entries,
                        "sources" to AppointmentSource.entries,
                        "error" to (e.message ?: "Ошибка")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            appointmentMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/appointments"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
