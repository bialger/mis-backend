package com.bialger.domain.posts

import com.bialger.domain.posts.repository.PostRepository
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.kotest.assertions.throwables.shouldThrow
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.core.spec.style.StringSpec

@MicronautTest(transactional = true)
@Property(name = "micronaut.http.client.follow-redirects", value = "false")
class PostWebControllerTest(
    @param:Client("/") private val client: HttpClient,
    private val postRepository: PostRepository
) : StringSpec({

    "GET posts renders list" {
        val html = client.toBlocking().retrieve("/posts")
        html shouldContain "Посты"
        html shouldContain "EventSource"
    }

    "GET post by id returns 404 when missing" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange("/posts/999999999", String::class.java)
        }
        ex.status shouldBe HttpStatus.NOT_FOUND
    }

    "POST create redirects to detail" {
        // '+' is space in x-www-form-urlencoded; use %2B for a literal plus in the title
        val request = HttpRequest.POST(
            "/posts",
            "title=Ctrl%2BTest&body=from+http"
        ).contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)

        val response: HttpResponse<String> = client.toBlocking().exchange(
            request,
            String::class.java
        )
        // HttpResponse.seeOther() — 303 SEE_OTHER to list (/posts), not detail
        response.status shouldBe HttpStatus.SEE_OTHER
        val location = response.header("Location") ?: error("no Location")
        location shouldBe "/posts"
        val entity = postRepository.findAllOrdered().firstOrNull { it.title == "Ctrl+Test" }.shouldNotBeNull()
        entity.body shouldBe "from http"
    }
})
