package com.bialger

import io.kotest.matchers.shouldBe
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.core.spec.style.StringSpec

@MicronautTest
class MisTest(
    private val application: EmbeddedApplication<*>,
    @param:Client("/") private val client: HttpClient
) : StringSpec({

    "test the server is running" {
        assert(application.isRunning)
    }

    "test root endpoint returns status message" {
        client.toBlocking().retrieve("/") shouldBe "MIS web server is running"
    }
})
