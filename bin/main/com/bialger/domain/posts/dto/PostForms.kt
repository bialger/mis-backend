package com.bialger.domain.posts.dto

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@Introspected
data class PostCreateForm(
    val title: String = "",
    val body: String = ""
)

@Serdeable
@Introspected
data class PostUpdateForm(
    val title: String = "",
    val body: String = ""
)
