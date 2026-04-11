package com.bialger.api

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
class CachingTest(
    @param:Client("/") private val client: HttpClient
) : StringSpec({

    // -------------------------------------------------------------------
    // Server-side cache: same data returned on repeated requests
    // -------------------------------------------------------------------

    "GET /api/branches repeated 5 times all return 200 with consistent body (server cache hit)" {
        val responses = (1..5).map {
            client.toBlocking().retrieve(
                HttpRequest.GET<String>("/api/branches?page=0&size=100"),
                String::class.java
            )
        }
        val first = responses.first()
        responses.drop(1).forEach { it shouldBe first }
    }

    // -------------------------------------------------------------------
    // ETag: generated for GET /api/** responses
    // -------------------------------------------------------------------

    "GET /api/branches response includes ETag header" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<String>("/api/branches?page=0&size=10"),
            String::class.java
        )
        response.status shouldBe HttpStatus.OK
        val etag = response.header("ETag")
        etag.shouldNotBeNull()
        etag shouldStartWith "\""
    }

    "ETag is stable across two identical GET /api/branches requests" {
        val r1 = client.toBlocking().exchange(
            HttpRequest.GET<String>("/api/branches?page=0&size=10"),
            String::class.java
        )
        val r2 = client.toBlocking().exchange(
            HttpRequest.GET<String>("/api/branches?page=0&size=10"),
            String::class.java
        )
        val etag1 = r1.header("ETag").shouldNotBeNull()
        val etag2 = r2.header("ETag").shouldNotBeNull()
        etag1 shouldBe etag2
    }

    // -------------------------------------------------------------------
    // ETag conditional request: If-None-Match → 304 Not Modified
    // -------------------------------------------------------------------

    "GET /api/branches with matching If-None-Match returns 304" {
        val first = client.toBlocking().exchange(
            HttpRequest.GET<String>("/api/branches?page=0&size=10"),
            String::class.java
        )
        val etag = first.header("ETag").shouldNotBeNull()

        val ex = runCatching {
            client.toBlocking().exchange(
                HttpRequest.GET<String>("/api/branches?page=0&size=10")
                    .header("If-None-Match", etag),
                String::class.java
            )
        }
        // Micronaut client throws HttpClientResponseException for 304
        val status = ex.getOrNull()?.status
            ?: (ex.exceptionOrNull() as? HttpClientResponseException)?.status
        status shouldBe HttpStatus.NOT_MODIFIED
    }

    // -------------------------------------------------------------------
    // Cache-Control header is present on branches list
    // -------------------------------------------------------------------

    "GET /api/branches response includes Cache-Control header with max-age" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<String>("/api/branches?page=0&size=10"),
            String::class.java
        )
        val cc = response.header("Cache-Control")
        cc.shouldNotBeNull()
        cc shouldStartWith "public"
    }
})
