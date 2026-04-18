package com.bialger.auth.domain

import com.bialger.domain.core.entity.EmployeeEntity
import io.micronaut.context.annotation.Value
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.cookie.SameSite
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.token.generator.TokenGenerator
import jakarta.inject.Singleton
import java.time.Duration

@Singleton
class JwtTokenService(
    private val tokenGenerator: TokenGenerator,
    @param:Value("\${micronaut.security.token.cookie.cookie-name:MIS_AUTH}")
    private val cookieName: String,
    @param:Value("\${micronaut.security.token.cookie.cookie-path:/}")
    private val cookiePath: String,
    @param:Value("\${micronaut.security.token.cookie.cookie-http-only:true}")
    private val cookieHttpOnly: Boolean,
    @param:Value("\${micronaut.security.token.cookie.cookie-secure:true}")
    private val cookieSecure: Boolean,
    @param:Value("\${micronaut.security.token.generator.access-token.expiration:28800}")
    private val accessTokenExpirationSeconds: Long
) {

    fun generateToken(employee: EmployeeEntity, roleCode: String): String {
        val identity = employee.email ?: employee.id.toString()
        val authentication = Authentication.build(
            identity,
            listOf(roleCode),
            mapOf(
                "employeeId" to employee.id.toString(),
                "role" to roleCode
            )
        )
        return tokenGenerator.generateToken(authentication, accessTokenExpirationSeconds.toInt())
            .orElseThrow { IllegalStateException("Could not generate JWT token") }
    }

    fun authCookie(token: String): Cookie =
        Cookie.of(cookieName, token)
            .path(cookiePath)
            .httpOnly(cookieHttpOnly)
            .secure(cookieSecure)
            .sameSite(SameSite.Lax)
            .maxAge(Duration.ofSeconds(accessTokenExpirationSeconds))

    fun clearCookie(): Cookie =
        Cookie.of(cookieName, "")
            .path(cookiePath)
            .httpOnly(cookieHttpOnly)
            .secure(cookieSecure)
            .sameSite(SameSite.Lax)
            .maxAge(Duration.ZERO)
}
