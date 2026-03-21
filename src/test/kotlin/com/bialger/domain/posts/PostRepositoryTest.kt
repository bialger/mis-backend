package com.bialger.domain.posts

import com.bialger.domain.posts.entity.PostEntity
import com.bialger.domain.posts.repository.PostRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.time.Instant

@MicronautTest(transactional = true)
class PostRepositoryTest(
    private val postRepository: PostRepository
) : StringSpec({

    "save assigns id and findAllOrdered returns newest first" {
        val a = postRepository.save(
            PostEntity(
                id = null,
                title = "A",
                body = "a",
                createdAt = Instant.parse("2025-01-01T10:00:00Z"),
                updatedAt = Instant.parse("2025-01-01T10:00:00Z")
            )
        )
        val b = postRepository.save(
            PostEntity(
                id = null,
                title = "B",
                body = "b",
                createdAt = Instant.parse("2025-01-02T10:00:00Z"),
                updatedAt = Instant.parse("2025-01-02T10:00:00Z")
            )
        )

        val idA = a.id.shouldNotBeNull()
        val idB = b.id.shouldNotBeNull()

        val ordered = postRepository.findAllOrdered().filter { it.id == idA || it.id == idB }
        ordered shouldHaveSize 2
        ordered.first().title shouldBe "B"
        ordered.last().title shouldBe "A"
    }
})
