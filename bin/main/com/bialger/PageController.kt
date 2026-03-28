package com.bialger

import com.bialger.web.CrmShellPageData
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.views.ModelAndView
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Controller
class PageController(
    private val crmShellPageData: CrmShellPageData
) {

    @Get("/")
    fun dashboard(): ModelAndView<Map<String, Any>> = appPage(
        title = "Медицинская CRM - Панель управления",
        activePage = "dashboard",
        contentView = "pages/content/dashboard",
        pageScript = "dashboard-app.js",
        extra = crmShellPageData.dashboardExtras()
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
        pageScript = "patients-app.js",
        extra = crmShellPageData.patientsExtras()
    )

    @Get("/patients/{id}")
    fun patientDetail(@PathVariable id: UUID): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Карточка пациента - Медицинская CRM",
        activePage = "patients",
        frontendFile = "patient-detail.html",
        pageScript = "patient-detail-app.js",
        extra = crmShellPageData.patientDetailExtras(id)
    )

    @Get("/doctors")
    fun doctors(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Врачи",
        activePage = "doctors",
        frontendFile = "doctors.html",
        pageScript = "doctors-app.js",
        extra = crmShellPageData.doctorsExtras()
    )

    @Get("/schedule")
    fun schedule(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Расписание",
        activePage = "schedule",
        frontendFile = "schedule.html",
        pageScript = "schedule-app.js",
        extra = crmShellPageData.scheduleExtras()
    )

    @Get("/reports")
    fun reports(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Отчеты",
        activePage = "reports",
        frontendFile = "reports.html",
        pageScript = "reports-app.js",
        extra = crmShellPageData.reportsExtras()
    )

    @Get("/inventory")
    fun inventory(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Склад",
        activePage = "inventory",
        frontendFile = "inventory.html",
        pageScript = "inventory-app.js",
        extra = crmShellPageData.inventoryExtras()
    )

    @Get("/audit")
    fun audit(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Логи действий",
        activePage = "audit",
        frontendFile = "audit.html",
        pageScript = "audit-app.js",
        extra = crmShellPageData.auditExtras()
    )

    @Get("/settings")
    fun settings(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM — Настройки",
        activePage = "settings",
        frontendFile = "settings.html",
        pageScript = "settings-app.js",
        extra = crmShellPageData.settingsExtras()
    )

    @Get("/appointments")
    fun appointments(): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Медицинская CRM - Записи на услуги",
        activePage = "appointments",
        frontendFile = "appointment.html",
        pageScript = "appointment-new-app.js",
        extra = crmShellPageData.appointmentsListExtras()
    )

    @Get("/appointments/{id}")
    fun appointmentDetail(@PathVariable id: UUID): ModelAndView<Map<String, Any>> = frontendPage(
        title = "Карточка записи - Медицинская CRM",
        activePage = "appointments",
        frontendFile = "appointment-detail.html",
        pageScript = "appointment-detail-app.js",
        extra = crmShellPageData.appointmentDetailExtras(id)
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
        val target = when (legacyPage) {
            "index" -> "/"
            "login" -> "/login"
            "patients" -> "/patients"
            "patient-detail" -> if (!id.isNullOrBlank()) "/patients/$id" else "/patients"
            "doctors" -> "/doctors"
            "schedule" -> "/schedule"
            "reports" -> "/reports"
            "inventory" -> "/inventory"
            "audit" -> "/audit"
            "settings" -> "/settings"
            "appointment" -> "/appointments"
            "appointment-detail" -> if (!id.isNullOrBlank()) "/appointments/$id" else "/appointments"
            "patient-booking" -> "/patient-booking"
            else -> "/"
        }
        return HttpResponse.seeOther(URI.create(target))
    }

    private fun frontendPage(
        title: String,
        activePage: String,
        frontendFile: String,
        pageScript: String,
        showHeader: Boolean = true,
        showFooter: Boolean = true,
        showSidebar: Boolean = true,
        extra: Map<String, Any> = emptyMap()
    ): ModelAndView<Map<String, Any>> = appPage(
        title = title,
        activePage = activePage,
        contentView = "pages/content/frontend-page",
        pageScript = pageScript,
        showHeader = showHeader,
        showFooter = showFooter,
        showSidebar = showSidebar,
        extra = mapOf("frontendSectionHtml" to loadFrontendSection(frontendFile)) + extra
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
