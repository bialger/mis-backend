package com.bialger.auth.web

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
import java.net.URI

@Filter("/**")
class HtmlUnauthorizedRedirectFilter : HttpServerFilter {

    override fun doFilter(
        request: HttpRequest<*>,
        chain: ServerFilterChain
    ): Publisher<MutableHttpResponse<*>> =
        Flux.from(chain.proceed(request)).map { response ->
            if (shouldRedirectToLogin(request, response)) {
                HttpResponse.seeOther<Any>(URI.create("/login"))
            } else {
                response
            }
        }

    private fun shouldRedirectToLogin(
        request: HttpRequest<*>,
        response: MutableHttpResponse<*>
    ): Boolean {
        if (response.status != HttpStatus.UNAUTHORIZED) return false
        if (request.method != HttpMethod.GET) return false
        val path = request.path
        if (path == "/login" || path == "/patient-booking") return false
        if (path.startsWith("/assets/")) return false
        if (path.startsWith("/api/")) return false
        if (path == "/graphql" || path.startsWith("/graphql/")) return false
        if (path == "/graphiql" || path.startsWith("/graphiql/")) return false
        return true
    }
}
