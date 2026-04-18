package com.bialger

import com.bialger.support.TestAuthHeaders
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.core.spec.style.StringSpec

@MicronautTest(transactional = false)
@Property(name = "micronaut.http.client.follow-redirects", value = "false")
class MisTest(
    private val application: EmbeddedApplication<*>,
    @param:Client("/") private val client: HttpClient
) : StringSpec({

    "test the server is running" {
        assert(application.isRunning)
    }

    "test root endpoint redirects to /login when unauthorized" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/")
                .header(TestAuthHeaders.NO_AUTH, "1"),
            String::class.java
        )
        response.status shouldBe HttpStatus.SEE_OTHER
        response.header("Location") shouldContain "/login"
    }

    "test frontend static css is served by micronaut" {
        val css = client.toBlocking().retrieve("/assets/css/base.css")
        css shouldContain ":root"
    }

    "test login page has footer without header and sidebar" {
        val html = client.toBlocking().retrieve("/login")
        html shouldContain "<footer class=\"l-footer\""
        html shouldNotContain "<header class=\"l-header\""
        html shouldNotContain "<aside class=\"m-aside\""
    }

    "test patient booking page has footer without header and sidebar" {
        val html = client.toBlocking().retrieve("/patient-booking")
        html shouldContain "<footer class=\"l-footer\""
        html shouldNotContain "<header class=\"l-header\""
        html shouldNotContain "<aside class=\"m-aside\""
    }
})
