package com.bialger.web

import io.micronaut.core.io.Writable
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.io.StringWriter

/**
 * Measures request processing time and reports it via the X-Elapsed-Time response header.
 *
 * For HTML (text/html) responses the filter also injects a small inline script that exposes
 * the server-side elapsed time as `window.__serverElapsedMs` so the Thymeleaf layout can
 * display it next to the client-side timing measured by the browser.
 *
 * For REST/GraphQL responses only the header is written (clients read it from the response).
 */
@Filter("/**")
class ElapsedTimeFilter : HttpServerFilter {

    private val log = LoggerFactory.getLogger(ElapsedTimeFilter::class.java)

    override fun doFilter(
        request: HttpRequest<*>,
        chain: ServerFilterChain
    ): Publisher<MutableHttpResponse<*>> {
        val startNano = System.nanoTime()
        return Flux.from(chain.proceed(request)).map { response ->
            val elapsedMs = (System.nanoTime() - startNano) / 1_000_000L
            log.debug("Request {} {} processed in {}ms", request.method, request.path, elapsedMs)
            response.header(HEADER_NAME, "${elapsedMs}ms")
            injectElapsedIntoHtml(response, elapsedMs)
            response
        }
    }

    private fun injectElapsedIntoHtml(
        response: MutableHttpResponse<*>,
        elapsedMs: Long
    ) {
        val contentType = response.contentType.orElse(null) ?: return
        if (!contentType.name.startsWith("text/html")) return
        val htmlString = resolveBodyAsString(response) ?: return
        if (!htmlString.contains("</body>", ignoreCase = true)) return
        val script = "<script>window.__serverElapsedMs=$elapsedMs;</script>"
        val modified = htmlString.replace("</body>", "$script</body>", ignoreCase = true)
        @Suppress("UNCHECKED_CAST")
        (response as MutableHttpResponse<Any>).body(modified)
    }

    private fun resolveBodyAsString(response: MutableHttpResponse<*>): String? {
        return when (val body = response.body()) {
            is String -> body
            is Writable -> runCatching {
                val sw = StringWriter()
                body.writeTo(sw)
                sw.toString()
            }.getOrNull()
            else -> null
        }
    }

    companion object {
        const val HEADER_NAME = "X-Elapsed-Time"
    }
}
