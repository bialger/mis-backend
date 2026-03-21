package com.bialger.domain.posts.web

import com.bialger.domain.posts.dto.PostCreateForm
import com.bialger.domain.posts.dto.PostUpdateForm
import com.bialger.domain.posts.service.PostService
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

@Controller("/posts")
class PostWebController(
    private val postService: PostService,
    private val domainEventSseHub: DomainEventSseHub,
    private val appPageModelFactory: AppPageModelFactory
) {

    @Get("/events", produces = [MediaType.TEXT_EVENT_STREAM])
    fun postEvents(): Publisher<Event<String>> = domainEventSseHub.stream("posts")

    @Get
    fun list(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Посты — Медицинская CRM",
            activePage = "posts",
            contentView = "pages/content/posts/list",
            extra = mapOf("posts" to postService.listAll())
        )

    @Get("/add")
    fun addForm(): ModelAndView<Map<String, Any>> =
        appPageModelFactory.appPage(
            title = "Новый пост",
            activePage = "posts",
            contentView = "pages/content/posts/form-add"
        )

    @Post
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(@Body form: PostCreateForm): HttpResponse<Any> {
        return try {
            val post = postService.create(form)
            val id = post.id ?: return HttpResponse.serverError("Не удалось получить id поста")
            HttpResponse.redirect(URI.create("/posts/$id"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Новый пост",
                    activePage = "posts",
                    contentView = "pages/content/posts/form-add",
                    extra = mapOf("error" to (e.message ?: "Ошибка валидации"))
                )
            )
        }
    }

    @Get("/{id}")
    fun detail(@PathVariable id: Long): HttpResponse<Any> {
        val post = postService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = post.title,
                activePage = "posts",
                contentView = "pages/content/posts/detail",
                extra = mapOf("post" to post)
            )
        )
    }

    @Get("/{id}/edit")
    fun editForm(@PathVariable id: Long): HttpResponse<Any> {
        val post = postService.getById(id) ?: return HttpResponse.notFound()
        return HttpResponse.ok(
            appPageModelFactory.appPage(
                title = "Редактирование: ${post.title}",
                activePage = "posts",
                contentView = "pages/content/posts/form-edit",
                extra = mapOf("post" to post)
            )
        )
    }

    @Patch("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun update(@PathVariable id: Long, @Body form: PostUpdateForm): HttpResponse<Any> {
        return try {
            postService.update(id, form)
            HttpResponse.redirect(URI.create("/posts/$id"))
        } catch (e: IllegalArgumentException) {
            val post = postService.getById(id)
            if (post == null) {
                return HttpResponse.notFound()
            }
            HttpResponse.ok(
                appPageModelFactory.appPage(
                    title = "Редактирование: ${post.title}",
                    activePage = "posts",
                    contentView = "pages/content/posts/form-edit",
                    extra = mapOf(
                        "post" to post,
                        "error" to (e.message ?: "Ошибка валидации")
                    )
                )
            )
        }
    }

    @Delete("/{id}")
    fun delete(@PathVariable id: Long): HttpResponse<Any> {
        return try {
            postService.delete(id)
            HttpResponse.redirect(URI.create("/posts"))
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }
}
