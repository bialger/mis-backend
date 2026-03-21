package com.bialger.domain.scheduling.mvc

import com.bialger.domain.mvc.MedicalServiceMvcForm
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

@Controller("/mvc/medical-services")
class MedicalServiceMvcController(
    private val medicalServiceMvcService: MedicalServiceMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(MedicalServiceMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Услуги (прайс)",
            activePage = "mvc",
            contentView = "pages/content/mvc/medical-services/list",
            extra = mapOf(
                "items" to medicalServiceMvcService.listAll(),
                "sseEventsPath" to "/mvc/medical-services/events",
                "sseEventName" to MedicalServiceMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новая услуга",
            activePage = "mvc",
            contentView = "pages/content/mvc/medical-services/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: MedicalServiceMvcForm): HttpResponse<Any> {
        return try {
            val price = MedicalServiceMvcService.parsePrice(form.price)
            val cost = MedicalServiceMvcService.parseCost(form.costPrice)
            val branch = form.branchId.parseUuidOrNull()
            val active = MedicalServiceMvcService.parseActiveFlag(form.isActive)
            val e = medicalServiceMvcService.create(form.name, price, cost, branch, active)
            HttpResponse.redirect(URI.create("/mvc/medical-services/${e.id}"))
        } catch (e: Exception) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новая услуга",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/medical-services/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = medicalServiceMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/medical-services/detail",
                extra = mapOf("item" to item)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = medicalServiceMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/medical-services/form-edit",
                extra = mapOf("item" to item)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: MedicalServiceMvcForm): HttpResponse<Any> {
        return try {
            val price = MedicalServiceMvcService.parsePrice(form.price)
            val cost = MedicalServiceMvcService.parseCost(form.costPrice)
            val branch = form.branchId.parseUuidOrNull()
            val active = MedicalServiceMvcService.parseActiveFlag(form.isActive)
            medicalServiceMvcService.update(id, form.name, price, cost, branch, active)
            HttpResponse.redirect(URI.create("/mvc/medical-services/$id"))
        } catch (e: Exception) {
            val item = medicalServiceMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/medical-services/form-edit",
                    extra = mapOf("item" to item, "error" to (e.message ?: "Ошибка"))
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            medicalServiceMvcService.delete(id)
            HttpResponse.redirect(URI.create("/mvc/medical-services"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
