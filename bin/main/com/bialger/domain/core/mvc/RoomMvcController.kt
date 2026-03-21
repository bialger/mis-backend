package com.bialger.domain.core.mvc

import com.bialger.domain.mvc.RoomMvcForm
import com.bialger.domain.mvc.formCheckboxOn
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

@Controller("/mvc/rooms")
class RoomMvcController(
    private val roomMvcService: RoomMvcService,
    private val branchMvcService: BranchMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(RoomMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Кабинеты (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/rooms/list",
            extra = mapOf(
                "items" to roomMvcService.listRows(),
                "sseEventsPath" to "/mvc/rooms/events",
                "sseEventName" to RoomMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый кабинет",
            activePage = "mvc",
            contentView = "pages/content/mvc/rooms/form-add",
            extra = mapOf("branches" to branchMvcService.listRows().map { it.branch })
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: RoomMvcForm): HttpResponse<Any> {
        return try {
            val bid = form.branchId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите филиал")
            val e = roomMvcService.create(
                bid,
                form.name,
                form.description,
                form.isActive.formCheckboxOn()
            )
            HttpResponse.seeOther(URI.create("/mvc/rooms"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый кабинет",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/rooms/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "branches" to branchMvcService.listRows().map { it.branch }
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = roomMvcService.getById(id) ?: return HttpResponse.notFound()
        val branchName = branchMvcService.getById(item.branchId)?.name
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/rooms/detail",
                extra = mapOf("item" to item, "branchName" to (branchName ?: ""))
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = roomMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/rooms/form-edit",
                extra = mapOf(
                    "item" to item,
                    "branches" to branchMvcService.listRows().map { it.branch }
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: RoomMvcForm): HttpResponse<Any> {
        return try {
            val bid = form.branchId.parseUuidOrNull() ?: throw IllegalArgumentException("Выберите филиал")
            roomMvcService.update(
                id,
                bid,
                form.name,
                form.description,
                form.isActive.formCheckboxOn()
            )
            HttpResponse.seeOther(URI.create("/mvc/rooms"))
        } catch (e: IllegalArgumentException) {
            val item = roomMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/rooms/form-edit",
                    extra = mapOf(
                        "item" to item,
                        "branches" to branchMvcService.listRows().map { it.branch },
                        "error" to (e.message ?: "Ошибка")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            roomMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/rooms"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
