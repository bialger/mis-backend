package com.bialger.domain.posts

import com.bialger.domain.posts.dto.PostCreateForm
import com.bialger.domain.posts.dto.PostUpdateForm
import com.bialger.domain.posts.repository.PostRepository
import com.bialger.domain.posts.service.PostService
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec

@MicronautTest(transactional = true)
class PostServiceTest(
    private val postService: PostService,
    private val postRepository: PostRepository
) : StringSpec({

    "create update delete" {
        val created = postService.create(PostCreateForm(title = " Заголовок ", body = " Текст "))
        val id = created.id.shouldNotBeNull()

        postService.getById(id)!!.title shouldBe "Заголовок"

        val updated = postService.update(id, PostUpdateForm(title = "Новый", body = "Да"))
        updated.title shouldBe "Новый"

        postService.delete(id)
        postService.getById(id).shouldBeNull()
        postRepository.findById(id).isPresent shouldBe false
    }

    "create rejects blank title" {
        shouldThrow<IllegalArgumentException> {
            postService.create(PostCreateForm(title = "   ", body = "x"))
        }
    }
})
