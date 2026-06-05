package com.bialger.web

import io.micronaut.core.order.Ordered
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.filter.ServerFilterPhase
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Provider
import org.reactivestreams.Publisher
import java.util.UUID

/**
 * Populates AuditRequestContext with HTTP metadata (IP, User-Agent, actor ID) once per request,
 * before the access-control filter runs. The audit service reads from this context so that IP and
 * User-Agent are captured without threading these values through every service method signature.
 */
@Filter("/api/**")
class AuditRequestContextFilter(
    private val contextProvider: Provider<AuditRequestContext>
) : HttpServerFilter, Ordered {

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        runCatching {
            val ctx = contextProvider.get()
            ctx.ipAddress = request.headers.get("X-Forwarded-For")
                ?.split(",")?.firstOrNull()?.trim()
                ?: request.remoteAddress?.address?.hostAddress
            ctx.userAgent = request.headers.get("User-Agent")
            val auth = request.getUserPrincipal(Authentication::class.java).orElse(null)
            ctx.actorId = auth?.attributes?.get("employeeId")?.toString()
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        }
        return chain.proceed(request)
    }

    override fun getOrder(): Int = ServerFilterPhase.SECURITY.after() + 5
}
