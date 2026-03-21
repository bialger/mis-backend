package com.bialger.domain.posts.service

import com.bialger.domain.posts.dto.PostCreateForm
import com.bialger.domain.posts.dto.PostUpdateForm
import com.bialger.domain.posts.entity.PostEntity
import com.bialger.domain.posts.repository.PostRepository
import com.bialger.web.DomainEventSseHub
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import java.time.Instant

@Singleton
class PostService(
    private val postRepository: PostRepository,
    private val domainEventSseHub: DomainEventSseHub,
    private val objectMapper: ObjectMapper
) {

    fun listAll(): List<PostEntity> = postRepository.findAllOrdered()

    fun getById(id: Long): PostEntity? = postRepository.findById(id).orElse(null)

    fun create(form: PostCreateForm): PostEntity {
        val title = form.title.trim()
        val body = form.body.trim()
        require(title.isNotEmpty()) { "Заголовок не может быть пустым" }

        val saved = postRepository.save(
            PostEntity(
                id = null,
                title = title,
                body = body,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val id = saved.id ?: error("Идентификатор поста не сгенерирован")
        emit(
            mapOf(
                "type" to "CREATED",
                "id" to id,
                "title" to saved.title
            )
        )
        return saved
    }

    fun update(id: Long, form: PostUpdateForm): PostEntity {
        val existing = postRepository.findById(id).orElseThrow { IllegalArgumentException("Пост не найден") }
        val title = form.title.trim()
        val body = form.body.trim()
        require(title.isNotEmpty()) { "Заголовок не может быть пустым" }

        val updated = existing.copy(
            title = title,
            body = body,
            updatedAt = Instant.now()
        )
        postRepository.update(updated)
        emit(
            mapOf(
                "type" to "UPDATED",
                "id" to id,
                "title" to updated.title
            )
        )
        return updated
    }

    fun delete(id: Long) {
        val existing = postRepository.findById(id).orElseThrow { IllegalArgumentException("Пост не найден") }
        postRepository.deleteById(id)
        emit(
            mapOf(
                "type" to "DELETED",
                "id" to id,
                "title" to existing.title
            )
        )
    }

    private fun emit(payload: Map<String, Any>) {
        domainEventSseHub.emitJson("posts", objectMapper.writeValueAsString(payload))
    }
}
