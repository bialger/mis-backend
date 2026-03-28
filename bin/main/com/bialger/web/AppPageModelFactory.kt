package com.bialger.web

import io.micronaut.views.ModelAndView
import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Singleton
class AppPageModelFactory {

    fun appPage(
        title: String,
        activePage: String,
        contentView: String,
        extra: Map<String, Any> = emptyMap()
    ): ModelAndView<Map<String, Any>> {
        val model = mutableMapOf<String, Any>(
            "title" to title,
            "activePage" to activePage,
            "contentView" to contentView,
            "contentFragment" to "content",
            "showHeader" to true,
            "showFooter" to true,
            "showSidebar" to true,
            "publicLayout" to false,
            "currentYear" to 2026,
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        )
        model.putAll(extra)
        return ModelAndView("pages/app", model)
    }
}
