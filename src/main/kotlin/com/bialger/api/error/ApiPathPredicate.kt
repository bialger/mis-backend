package com.bialger.api.error

import io.micronaut.http.HttpRequest

internal fun HttpRequest<*>.isApiRequest(): Boolean = path.startsWith("/api/")
