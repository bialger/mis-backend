package com.bialger.api.error

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ApiErrorResponse(
    val error: String,
    val message: String? = null,
    val violations: List<ViolationItem>? = null
) {
    @Serdeable
    data class ViolationItem(
        val path: String,
        val message: String
    )
}
