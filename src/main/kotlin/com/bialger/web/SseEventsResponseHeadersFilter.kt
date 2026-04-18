package com.bialger.web

import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

/**
 * Proxies (nginx) often buffer SSE; disable buffering. Safe for all long-lived event-stream responses.
 */
@Filter(value = ["/posts/events"])
class SseEventsResponseHeadersFilter : HttpServerFilter {

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> =
        Flux.from(chain.proceed(request)).map { response -> addSseHeaders(request, response) }
}

private fun addSseHeaders(request: HttpRequest<*>, response: MutableHttpResponse<*>): MutableHttpResponse<*> {
    if (request.method == HttpMethod.GET) {
        response.header("X-Accel-Buffering", "no")
        response.header("Cache-Control", "no-store, no-cache, must-revalidate")
    }
    return response
}
