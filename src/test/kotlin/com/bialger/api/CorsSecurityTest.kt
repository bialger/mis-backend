package com.bialger.api

import com.bialger.support.TestAuthHeaders
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
class CorsSecurityTest(
    @param:Client("/") private val client: HttpClient
) : StringSpec({

    fun <T> noAuth(request: MutableHttpRequest<T>): MutableHttpRequest<T> =
        request.header(TestAuthHeaders.NO_AUTH, "1")

    "preflight from allowed origin returns CORS headers" {
        val origin = "http://localhost:3000"
        val response = client.toBlocking().exchange(
            noAuth(
                HttpRequest.OPTIONS<Any>("/api/patients")
                    .header("Origin", origin)
                    .header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "Authorization,Content-Type")
            ),
            String::class.java
        )

        (response.status == HttpStatus.OK || response.status == HttpStatus.NO_CONTENT) shouldBe true
        response.header("Access-Control-Allow-Origin").shouldNotBeNull() shouldBe origin
        response.header("Access-Control-Allow-Credentials").shouldNotBeNull() shouldBe "true"
        response.header("Access-Control-Allow-Methods").shouldNotBeNull() shouldContain "GET"
    }

    "simple request from allowed origin returns CORS headers" {
        val origin = "http://localhost:5173"
        val response = client.toBlocking().exchange(
            noAuth(
                HttpRequest.GET<Any>("/api/public/booking/branches")
                    .header("Origin", origin)
            ),
            String::class.java
        )
        response.status shouldBe HttpStatus.OK
        val allowOrigin = response.header("Access-Control-Allow-Origin").shouldNotBeNull()
        allowOrigin shouldBe origin
        response.header("Access-Control-Allow-Credentials") shouldBe "true"
        (allowOrigin != "*") shouldBe true
    }

    "preflight from disallowed origin is not allowed" {
        val request = noAuth(
            HttpRequest.OPTIONS<Any>("/api/patients")
                .header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "GET")
        )

        val response: HttpResponse<*> = try {
            client.toBlocking().exchange(request, String::class.java)
        } catch (e: HttpClientResponseException) {
            e.response
        }
        val allowOrigin = response.header("Access-Control-Allow-Origin")
        (allowOrigin == null || allowOrigin.isBlank()) shouldBe true
    }
})
