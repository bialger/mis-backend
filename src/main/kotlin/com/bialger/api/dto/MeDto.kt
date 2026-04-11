package com.bialger.api.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.media.Schema

@Serdeable
@Introspected
@Schema(description = "Current user info")
data class MeUserDto(
    val id: String,
    val name: String,
    val role: String,
    val login: String
)

@Serdeable
@Introspected
@Schema(description = "Current session: user, branch scope, permissions")
data class MeRestDto(
    val user: MeUserDto,
    val branchScope: List<String>,
    val permissions: Map<String, Any>
)
