package com.bialger.domain.laboratory.mvc

import com.bialger.domain.laboratory.repository.LaboratoryRepository
import com.bialger.domain.mvc.LabTestMvcForm
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

@Controller("/mvc/lab-tests")
class LabTestMvcController(
    private val labTestMvcService: LabTestMvcService,
    private val laboratoryRepository: LaboratoryRepository,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(LabTestMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Лабораторные тесты (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/lab-tests/list",
            extra = mapOf(
                "items" to labTestMvcService.listRows(),
                "sseEventsPath" to "/mvc/lab-tests/events",
                "sseEventName" to LabTestMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый лаб. тест",
            activePage = "mvc",
            contentView = "pages/content/mvc/lab-tests/form-add",
            extra = mapOf("laboratories" to laboratoryRepository.findAllOrdered())
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: LabTestMvcForm): HttpResponse<Any> {
        return try {
            val labId = form.laboratoryId.parseUuidOrNull()
            val e = labTestMvcService.create(
                form.name,
                form.description,
                LabTestMvcService.parsePrice(form.price),
                labId,
                LabTestMvcService.parseActive(form.isActive)
            )
            HttpResponse.seeOther(URI.create("/mvc/lab-tests"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый лаб. тест",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/lab-tests/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "laboratories" to laboratoryRepository.findAllOrdered()
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = labTestMvcService.getById(id) ?: return HttpResponse.notFound()
        val labName = item.laboratoryId?.let { lid ->
            laboratoryRepository.findById(lid).orElse(null)?.name
        }
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/lab-tests/detail",
                extra = mapOf("item" to item, "laboratoryName" to (labName ?: ""))
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = labTestMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/lab-tests/form-edit",
                extra = mapOf(
                    "item" to item,
                    "laboratories" to laboratoryRepository.findAllOrdered()
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: LabTestMvcForm): HttpResponse<Any> {
        return try {
            val labId = form.laboratoryId.parseUuidOrNull()
            labTestMvcService.update(
                id,
                form.name,
                form.description,
                LabTestMvcService.parsePrice(form.price),
                labId,
                LabTestMvcService.parseActive(form.isActive)
            )
            HttpResponse.seeOther(URI.create("/mvc/lab-tests"))
        } catch (e: Exception) {
            val item = labTestMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/lab-tests/form-edit",
                    extra = mapOf(
                        "item" to item,
                        "laboratories" to laboratoryRepository.findAllOrdered(),
                        "error" to (e.message ?: "Ошибка")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            labTestMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/lab-tests"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
