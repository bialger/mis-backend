package com.bialger.api.util

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import kotlin.math.min

object ApiPage {
    fun <T> slice(all: List<T>, pageable: Pageable): Page<T> {
        val total = all.size.toLong()
        val offset = pageable.offset.toInt().coerceAtLeast(0)
        val end = min(offset + pageable.size, all.size)
        val slice = if (offset < all.size) all.subList(offset, end) else emptyList()
        return Page.of(slice, pageable, total)
    }
}
