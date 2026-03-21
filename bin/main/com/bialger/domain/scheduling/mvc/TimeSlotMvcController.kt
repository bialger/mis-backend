package com.bialger.domain.scheduling.mvc

import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.mvc.TimeSlotMvcForm
import com.bialger.domain.mvc.employeeDropdown
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

@Controller("/mvc/time-slots")
class TimeSlotMvcController(
    private val timeSlotMvcService: TimeSlotMvcService,
    private val employeeRepository: EmployeeRepository,
    private val roomRepository: RoomRepository,
    private val branchRepository: BranchRepository,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(TimeSlotMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Слоты расписания (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/time-slots/list",
            extra = mapOf(
                "rows" to timeSlotMvcService.listRows(),
                "sseEventsPath" to "/mvc/time-slots/events",
                "sseEventName" to TimeSlotMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый слот",
            activePage = "mvc",
            contentView = "pages/content/mvc/time-slots/form-add",
            extra = mapOf(
                "employees" to employeeDropdown(employeeRepository, null),
                "rooms" to roomRepository.findAllOrdered(),
                "branches" to branchRepository.findAllOrdered()
            )
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: TimeSlotMvcForm): HttpResponse<Any> {
        return try {
            val emp = form.employeeId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите сотрудника")
            val room = form.roomId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите кабинет")
            val br = form.branchId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите филиал")
            val e = timeSlotMvcService.create(
                emp,
                room,
                br,
                TimeSlotMvcService.parseLocalDate(form.slotDate),
                TimeSlotMvcService.parseLocalTime(form.startTime),
                TimeSlotMvcService.parseLocalTime(form.endTime),
                TimeSlotMvcService.parseIsAvailable(form.isAvailable)
            )
            HttpResponse.seeOther(URI.create("/mvc/time-slots"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый слот",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/time-slots/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "employees" to employeeDropdown(employeeRepository, null),
                        "rooms" to roomRepository.findAllOrdered(),
                        "branches" to branchRepository.findAllOrdered()
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val slot = timeSlotMvcService.getById(id) ?: return HttpResponse.notFound()
        val row = timeSlotMvcService.listRows().find { it.slot.id == id }
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Слот ${slot.slotDate}",
                activePage = "mvc",
                contentView = "pages/content/mvc/time-slots/detail",
                extra = mapOf(
                    "slot" to slot,
                    "employeeName" to (row?.employeeName ?: ""),
                    "roomName" to (row?.roomName ?: ""),
                    "branchName" to (row?.branchName ?: "")
                )
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val slot = timeSlotMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование слота",
                activePage = "mvc",
                contentView = "pages/content/mvc/time-slots/form-edit",
                extra = mapOf(
                    "slot" to slot,
                    "employees" to employeeDropdown(employeeRepository, slot.employeeId),
                    "rooms" to roomRepository.findAllOrdered(),
                    "branches" to branchRepository.findAllOrdered()
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: TimeSlotMvcForm): HttpResponse<Any> {
        return try {
            val emp = form.employeeId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите сотрудника")
            val room = form.roomId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите кабинет")
            val br = form.branchId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите филиал")
            timeSlotMvcService.update(
                id,
                emp,
                room,
                br,
                TimeSlotMvcService.parseLocalDate(form.slotDate),
                TimeSlotMvcService.parseLocalTime(form.startTime),
                TimeSlotMvcService.parseLocalTime(form.endTime),
                TimeSlotMvcService.parseIsAvailable(form.isAvailable)
            )
            HttpResponse.seeOther(URI.create("/mvc/time-slots"))
        } catch (e: Exception) {
            val slot = timeSlotMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование слота",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/time-slots/form-edit",
                    extra = mapOf(
                        "slot" to slot,
                        "employees" to employeeDropdown(employeeRepository, slot.employeeId),
                        "rooms" to roomRepository.findAllOrdered(),
                        "branches" to branchRepository.findAllOrdered(),
                        "error" to (e.message ?: "Ошибка")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            timeSlotMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/time-slots"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
