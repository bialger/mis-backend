package com.bialger

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.views.ModelAndView
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Controller
class PageController {

    @Get("/")
    fun dashboard(): ModelAndView<Map<String, Any>> = appPage(
        title = "Медицинская CRM - Панель управления",
        activePage = "dashboard",
        contentView = "pages/content/dashboard",
        pageScript = "dashboard-app.js"
    )

    @Get("/login")
    fun login(): ModelAndView<Map<String, Any>> = publicPage(
        title = "Вход в систему - Медицинская CRM",
        activePage = "login",
        contentView = "pages/content/login",
        pageScript = "login-app.js",
        showFooter = true
    )

    @Get("/patients")
    fun patients(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Пациенты",
        activePage = "patients",
        frontendFile = "patients.html",
        pageScript = "patients-app.js"
    )

    @Get("/patients/{id}")
    fun patientDetail(id: Long): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Карточка пациента - Медицинская CRM",
        activePage = "patients",
        frontendFile = "patient-detail.html",
        pageScript = "patient-detail-app.js"
    )

    @Get("/doctors")
    fun doctors(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Врачи",
        activePage = "doctors",
        frontendFile = "doctors.html",
        pageScript = "doctors-app.js"
    )

    @Get("/schedule")
    fun schedule(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Расписание",
        activePage = "schedule",
        frontendFile = "schedule.html",
        pageScript = "schedule-app.js"
    )

    @Get("/reports")
    fun reports(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Отчеты",
        activePage = "reports",
        frontendFile = "reports.html",
        pageScript = "reports-app.js"
    )

    @Get("/inventory")
    fun inventory(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Склад",
        activePage = "inventory",
        frontendFile = "inventory.html",
        pageScript = "inventory-app.js"
    )

    @Get("/audit")
    fun audit(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Логи действий",
        activePage = "audit",
        frontendFile = "audit.html",
        pageScript = "audit-app.js"
    )

    @Get("/settings")
    fun settings(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Настройки",
        activePage = "settings",
        frontendFile = "settings.html",
        pageScript = "settings-app.js"
    )

    @Get("/appointments")
    fun appointments(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Записи на услуги",
        activePage = "appointments",
        frontendFile = "appointment.html",
        pageScript = "appointment-new-app.js"
    )

    @Get("/appointments/{id}")
    fun appointmentDetail(id: Long): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Карточка записи - Медицинская CRM",
        activePage = "appointments",
        frontendFile = "appointment-detail.html",
        pageScript = "appointment-detail-app.js"
    )

    @Get("/patient-booking")
    fun patientBooking(): ModelAndView<Map<String, Any>> = publicPage(
        title = "Онлайн-запись к врачу",
        activePage = "appointments",
        contentView = "pages/content/patient-booking",
        pageScript = "patient-booking.js",
        showFooter = true
    )

    @Get("/{legacyPage}.html")
    fun legacyRoutes(
        legacyPage: String,
        @QueryValue("id") id: String?
    ): HttpResponse<Any> {
        val idLong = id?.toLongOrNull()
        val target = when (legacyPage) {
            "index" -> "/"
            "login" -> "/login"
            "patients" -> "/patients"
            "patient-detail" -> if (idLong != null) "/patients/$idLong" else "/patients"
            "doctors" -> "/doctors"
            "schedule" -> "/schedule"
            "reports" -> "/reports"
            "inventory" -> "/inventory"
            "audit" -> "/audit"
            "settings" -> "/settings"
            "appointment" -> "/appointments"
            "appointment-detail" -> if (idLong != null) "/appointments/$idLong" else "/appointments"
            "patient-booking" -> "/patient-booking"
            else -> "/"
        }
        return HttpResponse.redirect(URI.create(target))
    }

    private fun frontendPage(
        title: String,
        activePage: String,
        frontendFile: String,
        pageScript: String,
        showHeader: Boolean = true,
        showFooter: Boolean = true,
        showSidebar: Boolean = true
    ): ModelAndView<Map<String, Any>> = appPage(
        title = title,
        activePage = activePage,
        contentView = "pages/content/frontend-page",
        pageScript = pageScript,
        showHeader = showHeader,
        showFooter = showFooter,
        showSidebar = showSidebar,
        extra = mapOf("frontendSectionHtml" to loadFrontendSection(frontendFile))
    )

    private fun appPage(
        title: String,
        activePage: String,
        contentView: String,
        contentFragment: String = "content",
        pageScript: String? = null,
        showHeader: Boolean = true,
        showFooter: Boolean = true,
        showSidebar: Boolean = true,
        publicLayout: Boolean = false,
        extra: Map<String, Any> = emptyMap()
    ): ModelAndView<Map<String, Any>> {
        val model = mutableMapOf<String, Any>(
            "title" to title,
            "activePage" to activePage,
            "contentView" to contentView,
            "contentFragment" to contentFragment,
            "showHeader" to showHeader,
            "showFooter" to showFooter,
            "showSidebar" to showSidebar,
            "publicLayout" to publicLayout,
            "currentYear" to 2026,
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        )

        if (pageScript != null) {
            model["pageScript"] = pageScript
        }
        model.putAll(extra)

        return ModelAndView("pages/app", model)
    }

    private fun publicPage(
        title: String,
        activePage: String,
        contentView: String,
        contentFragment: String = "content",
        pageScript: String? = null,
        showFooter: Boolean = true,
        extra: Map<String, Any> = emptyMap()
    ): ModelAndView<Map<String, Any>> {
        val model = mutableMapOf<String, Any>(
            "title" to title,
            "activePage" to activePage,
            "contentView" to contentView,
            "contentFragment" to contentFragment,
            "showFooter" to showFooter,
            "currentYear" to 2026,
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        )

        if (pageScript != null) {
            model["pageScript"] = pageScript
        }
        model.putAll(extra)

        return ModelAndView("pages/public", model)
    }

    private fun loadFrontendSection(frontendFile: String): String {
        val resourcePath = "/static/pages/$frontendFile"
        val inputStream = javaClass.getResourceAsStream(resourcePath) ?: return """
            <section class="m-section">
                <div class="m-error-message">Шаблон страницы не найден: $frontendFile</div>
            </section>
        """.trimIndent()

        val html = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val mainContent = extractByMarkers(
            html = html,
            startMarker = "<div class=\"l-main-content\">",
            endMarker = "<div id=\"sidebar-placeholder\"></div>"
        )
        if (mainContent != null) {
            return mainContent
        }

        val sectionMatch = Regex("(?is)<section\\b.*?</section>").find(html)
        return sectionMatch?.value ?: """
            <section class="m-section">
                <div class="m-error-message">Не удалось извлечь контент страницы: $frontendFile</div>
            </section>
        """.trimIndent()
    }

    private fun extractByMarkers(html: String, startMarker: String, endMarker: String): String? {
        val start = html.indexOf(startMarker)
        if (start == -1) {
            return null
        }
        val end = html.indexOf(endMarker, start + startMarker.length)
        if (end == -1) {
            return null
        }
        return html.substring(start, end).trim()
    }
}
