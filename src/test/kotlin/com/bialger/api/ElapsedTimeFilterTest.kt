package com.bialger.api

import com.bialger.web.ElapsedTimeFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
class ElapsedTimeFilterTest(
    @param:Client("/") private val client: HttpClient
) : StringSpec({

    "GET /api/branches returns X-Elapsed-Time header" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<String>("/api/branches?page=0&size=1"),
            String::class.java
        )
        val header = response.header(ElapsedTimeFilter.HEADER_NAME)
        header.shouldNotBeNull()
        header shouldEndWith "ms"
    }

    "GET / (HTML page) returns X-Elapsed-Time header" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<String>("/"),
            String::class.java
        )
        val header = response.header(ElapsedTimeFilter.HEADER_NAME)
        header.shouldNotBeNull()
        header shouldEndWith "ms"
    }

    "GET / (HTML page) body contains injected serverElapsedMs script" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<String>("/"),
            String::class.java
        )
        val body = response.body()
        body.shouldNotBeNull()
        body shouldContain "window.__serverElapsedMs="
    }

    "X-Elapsed-Time value is a non-negative number followed by 'ms'" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<String>("/api/branches?page=0&size=1"),
            String::class.java
        )
        val header = response.header(ElapsedTimeFilter.HEADER_NAME).shouldNotBeNull()
        val ms = header.removeSuffix("ms").toLongOrNull()
        ms.shouldNotBeNull()
        (ms >= 0) shouldBe true
    }
})
