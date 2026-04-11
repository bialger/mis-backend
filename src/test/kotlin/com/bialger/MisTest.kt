package com.bialger

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.core.spec.style.StringSpec

@MicronautTest(transactional = false)
class MisTest(
    private val application: EmbeddedApplication<*>,
    @param:Client("/") private val client: HttpClient
) : StringSpec({

    "test the server is running" {
        assert(application.isRunning)
    }

    "test root endpoint renders dashboard page" {
        val html = client.toBlocking().retrieve("/")
        html shouldContain "Панель управления"
        html shouldContain "Медицинская CRM система"
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
