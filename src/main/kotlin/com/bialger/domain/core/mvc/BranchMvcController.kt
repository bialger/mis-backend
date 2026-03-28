package com.bialger.domain.core.mvc

import com.bialger.domain.mvc.BranchMvcForm
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
import io.swagger.v3.oas.annotations.Hidden

@Hidden
@Controller("/mvc/branches")
class BranchMvcController(
    private val branchMvcService: BranchMvcService,
    private val organizationMvcService: OrganizationMvcService,
    private val appPageModelFactory: AppPageModelFactory,
    private val domainEventSseHub: DomainEventSseHub
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun events(): Publisher<Event<String>> = domainEventSseHub.stream(BranchMvcService.TOPIC)

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Филиалы (MVC)",
            activePage = "mvc",
            contentView = "pages/content/mvc/branches/list",
            extra = mapOf(
                "items" to branchMvcService.listRows(),
                "sseEventsPath" to "/mvc/branches/events",
                "sseEventName" to BranchMvcService.EVENT_NAME
            )
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый филиал",
            activePage = "mvc",
            contentView = "pages/content/mvc/branches/form-add",
            extra = mapOf("organizations" to organizationMvcService.listAll())
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: BranchMvcForm): HttpResponse<Any> {
        return try {
            val orgId = form.organizationId.parseUuidOrNull()
                ?: throw IllegalArgumentException("Выберите организацию")
            val e = branchMvcService.create(
                orgId,
                form.name,
                form.address,
                form.phone,
                form.isActive.formCheckboxOn()
            )
            HttpResponse.seeOther(URI.create("/mvc/branches"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый филиал",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/branches/form-add",
                    extra = mapOf(
                        "error" to (e.message ?: "Ошибка"),
                        "organizations" to organizationMvcService.listAll()
                    )
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: UUID): HttpResponse<Any> {
        val item = branchMvcService.getById(id) ?: return HttpResponse.notFound()
        val orgName = organizationMvcService.getById(item.organizationId)?.name
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = item.name,
                activePage = "mvc",
                contentView = "pages/content/mvc/branches/detail",
                extra = mapOf("item" to item, "organizationName" to (orgName ?: ""))
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: UUID): HttpResponse<Any> {
        val item = branchMvcService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${item.name}",
                activePage = "mvc",
                contentView = "pages/content/mvc/branches/form-edit",
                extra = mapOf(
                    "item" to item,
                    "organizations" to organizationMvcService.listAll()
                )
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: UUID, @Body form: BranchMvcForm): HttpResponse<Any> {
        return try {
            val orgId = form.organizationId.parseUuidOrNull()
                ?: throw IllegalArgumentException("Выберите организацию")
            branchMvcService.update(
                id,
                orgId,
                form.name,
                form.address,
                form.phone,
                form.isActive.formCheckboxOn()
            )
            HttpResponse.seeOther(URI.create("/mvc/branches"))
        } catch (e: IllegalArgumentException) {
            val item = branchMvcService.getById(id) ?: return HttpResponse.notFound()
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${item.name}",
                    activePage = "mvc",
                    contentView = "pages/content/mvc/branches/form-edit",
                    extra = mapOf(
                        "item" to item,
                        "organizations" to organizationMvcService.listAll(),
                        "error" to (e.message ?: "Ошибка")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: UUID): HttpResponse<Any> {
        return try {
            branchMvcService.delete(id)
            HttpResponse.seeOther(URI.create("/mvc/branches"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
