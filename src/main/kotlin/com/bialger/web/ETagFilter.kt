package com.bialger.web

import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.security.MessageDigest

// Generates a strong ETag (SHA-256 of the response body) for GET responses under /api/**.
// If the request carries an If-None-Match header that matches the computed ETag the filter
// short-circuits with HTTP 304 Not Modified, saving bandwidth.
// Cache-Control headers are left to individual controllers (branches/catalog use max-age=3600).
@Filter("/api/**")
class ETagFilter : HttpServerFilter {

    override fun doFilter(
        request: HttpRequest<*>,
        chain: ServerFilterChain
    ): Publisher<MutableHttpResponse<*>> {
        if (request.method != HttpMethod.GET) {
            return chain.proceed(request)
        }
        return Flux.from(chain.proceed(request)).map { response ->
            addETag(request, response)
        }
    }

    private fun addETag(
        request: HttpRequest<*>,
        response: MutableHttpResponse<*>
    ): MutableHttpResponse<*> {
        if (response.status.code !in 200..299) return response
        val body = response.body() ?: return response
        val bodyBytes = when (body) {
            is ByteArray -> body
            is String -> body.toByteArray(Charsets.UTF_8)
            else -> body.toString().toByteArray(Charsets.UTF_8)
        }
        if (bodyBytes.isEmpty()) return response

        val etag = "\"${sha256Hex(bodyBytes)}\""
        response.header("ETag", etag)

        val ifNoneMatch = request.headers["If-None-Match"]
        if (ifNoneMatch != null && etagMatches(ifNoneMatch, etag)) {
            return HttpResponse.status<Any>(HttpStatus.NOT_MODIFIED)
                .header("ETag", etag)
        }
        return response
    }

    private fun etagMatches(ifNoneMatch: String, etag: String): Boolean {
        if (ifNoneMatch.trim() == "*") return true
        return ifNoneMatch.split(",").map { it.trim() }.any { it == etag }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val sb = StringBuilder()
        for (b in digest.digest(bytes)) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
