package com.bialger.domain.mvc

import java.time.LocalTime
import java.util.UUID

internal fun String?.formCheckboxOn(): Boolean = this == "on" || this == "true" || this == "1"

internal fun String?.parseUuidOrNull(): UUID? =
    this?.trim()?.takeIf { it.isNotEmpty() }?.let { UUID.fromString(it) }

internal fun List<String>?.parseUuidList(): List<UUID> =
    this.orEmpty().mapNotNull { s ->
        s.trim().takeIf { it.isNotEmpty() }?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    }

/** Empty both → 08:00–20:00; both set → parsed; one set → error. */
internal fun parseBranchHoursOrDefault(startRaw: String?, endRaw: String?): Pair<LocalTime, LocalTime> {
    val s = startRaw?.trim().orEmpty()
    val e = endRaw?.trim().orEmpty()
    if (s.isEmpty() && e.isEmpty()) return LocalTime.of(8, 0) to LocalTime.of(20, 0)
    if (s.isEmpty() || e.isEmpty()) {
        throw IllegalArgumentException(
            "Укажите время работы филиала с и до (или оставьте оба поля пустыми для 08:00–20:00)"
        )
    }
    val st = runCatching { LocalTime.parse(s) }.getOrElse {
        throw IllegalArgumentException("Некорректное время начала работы филиала")
    }
    val en = runCatching { LocalTime.parse(e) }.getOrElse {
        throw IllegalArgumentException("Некорректное время окончания работы филиала")
    }
    require(en > st) { "Время окончания работы филиала должно быть позже начала" }
    return st to en
}

/** Empty both → nulls (use branch hours in booking); both set → parsed; one set → error. */
internal fun parseOptionalEmployeeWorkHours(workStart: String?, workEnd: String?): Pair<LocalTime?, LocalTime?> {
    val s = workStart?.trim().orEmpty()
    val e = workEnd?.trim().orEmpty()
    if (s.isEmpty() && e.isEmpty()) return null to null
    if (s.isEmpty() || e.isEmpty()) {
        throw IllegalArgumentException(
            "Укажите время приёма с и до (или оставьте оба поля пустыми — действует расписание филиала)"
        )
    }
    val st = runCatching { LocalTime.parse(s) }.getOrElse {
        throw IllegalArgumentException("Некорректное время начала приёма")
    }
    val en = runCatching { LocalTime.parse(e) }.getOrElse {
        throw IllegalArgumentException("Некорректное время окончания приёма")
    }
    require(en > st) { "Время окончания приёма должно быть позже начала" }
    return st to en
}
