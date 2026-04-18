package com.bialger.auth.web

import com.bialger.api.error.ApiErrorResponse
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

@Filter("/api/**")
class ApiAccessControlFilter(
    private val currentUserContextService: CurrentUserContextService,
    private val accessControlService: AccessControlService
) : HttpServerFilter, Ordered {

    override fun doFilter(
        request: HttpRequest<*>,
        chain: ServerFilterChain
    ): Publisher<MutableHttpResponse<*>> {
        if (request.method == HttpMethod.OPTIONS) {
            return chain.proceed(request)
        }
        val authentication = request.getUserPrincipal(Authentication::class.java).orElse(null)
        val context = currentUserContextService.resolve(authentication)
            ?: return chain.proceed(request)
        if (isGrafanaApiPath(request.path) && context.roleCode != "SYSADMIN") {
            return Flux.just(
                HttpResponse.status<ApiErrorResponse>(HttpStatus.FORBIDDEN).body(
                    ApiErrorResponse(
                        error = "forbidden",
                        message = "Grafana access is allowed only for SYSADMIN"
                    )
                )
            )
        }
        if (isAccessControlApiPath(request.path)) {
            val canReadAccess = context.roleCode == "SYSADMIN" || context.roleCode == "HEAD"
            val canWriteAccess = context.roleCode == "SYSADMIN"
            val allowed = when (request.method) {
                HttpMethod.GET -> canReadAccess
                else -> canWriteAccess
            }
            if (!allowed) {
                return Flux.just(
                    HttpResponse.status<ApiErrorResponse>(HttpStatus.FORBIDDEN).body(
                        ApiErrorResponse(
                            error = "forbidden",
                            message = "Access permissions are available only for SYSADMIN and HEAD (write: SYSADMIN only)"
                        )
                    )
                )
            }
        }
        val requiredCode = accessControlService.requiredApiPermission(
            path = request.path,
            method = request.methodName,
            shellKind = request.parameters.get("kind")
        ) ?: return chain.proceed(request)
        val grantedCodes = accessControlService.resolveEffectivePermissionCodes(context.employee)
        if (requiredCode in grantedCodes) {
            return chain.proceed(request)
        }
        return Flux.just(
            HttpResponse.status<ApiErrorResponse>(HttpStatus.FORBIDDEN).body(
                ApiErrorResponse(
                    error = "forbidden",
                    message = "Permission '$requiredCode' is required"
                )
            )
        )
    }

    override fun getOrder(): Int = ServerFilterPhase.SECURITY.after() + 10

    private fun isGrafanaApiPath(path: String): Boolean =
        path == "/api/grafana" || path.startsWith("/api/grafana/")

    private fun isAccessControlApiPath(path: String): Boolean =
        path == "/api/access" || path.startsWith("/api/access/")
}
