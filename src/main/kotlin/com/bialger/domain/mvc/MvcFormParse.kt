package com.bialger.domain.mvc

import java.util.UUID

internal fun String?.formCheckboxOn(): Boolean = this == "on" || this == "true" || this == "1"

internal fun String?.parseUuidOrNull(): UUID? =
    this?.trim()?.takeIf { it.isNotEmpty() }?.let { UUID.fromString(it) }
