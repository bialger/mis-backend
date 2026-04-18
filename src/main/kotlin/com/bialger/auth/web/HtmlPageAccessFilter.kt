package com.bialger.auth.web

import com.bialger.auth.application.AccessControlService
import com.bialger.auth.application.CurrentUserContextService
import io.micronaut.core.order.Ordered
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterPhase
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.security.authentication.Authentication
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.net.URI

@Filter("/**")
class HtmlPageAccessFilter(
    private val currentUserContextService: CurrentUserContextService,
    private val accessControlService: AccessControlService
) : HttpServerFilter, Ordered {

    override fun doFilter(
        request: HttpRequest<*>,
        chain: ServerFilterChain
    ): Publisher<MutableHttpResponse<*>> {
        if (request.method != HttpMethod.GET) return chain.proceed(request)
        if (!isInternalHtmlPath(request.path)) return chain.proceed(request)

        val authentication = request.getUserPrincipal(Authentication::class.java).orElse(null)
        val context = currentUserContextService.resolve(authentication)
            ?: return chain.proceed(request)
        if (isGrafanaPath(request.path) && context.roleCode != "SYSADMIN") {
            return Flux.just(HttpResponse.status<Any>(HttpStatus.FORBIDDEN))
        }
        if (accessControlService.canAccessHtmlPath(request.path, context.permissions)) {
            return chain.proceed(request)
        }
        val canOpenSettings = context.permissions["canViewSettings"] as? Boolean == true
        val target = if (canOpenSettings && request.path != "/settings") "/settings" else "/login"
        return Flux.just(HttpResponse.seeOther<Any>(URI.create(target)))
    }

    private fun isInternalHtmlPath(path: String): Boolean {
        if (path == "/login" || path == "/patient-booking") return false
        if (path.startsWith("/assets/")) return false
        if (path.startsWith("/api/")) return false
        if (path == "/graphql" || path.startsWith("/graphql/")) return false
        if (path == "/graphiql" || path.startsWith("/graphiql/")) return false
        if (path == "/swagger" || path.startsWith("/swagger/")) return false
        if (path == "/swagger-ui" || path.startsWith("/swagger-ui/")) return false
        if (path == "/favicon.ico") return false
        return true
    }

    private fun isGrafanaPath(path: String): Boolean =
        path == "/grafana" || path.startsWith("/grafana/")

    override fun getOrder(): Int = ServerFilterPhase.SECURITY.after() + 20
}
