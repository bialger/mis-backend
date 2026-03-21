package com.bialger.domain.posts.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant

@MappedEntity("crm_post")
data class PostEntity(
    @field:Id
    @field:GeneratedValue(GeneratedValue.Type.IDENTITY)
    val id: Long? = null,
    val title: String,
    val body: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
