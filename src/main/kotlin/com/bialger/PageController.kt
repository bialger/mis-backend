package com.bialger

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.views.ModelAndView
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Controller
class PageController {

    @Get("/")
    fun dashboard(): ModelAndView<Map<String, Any>> = appPage(
        title = "Медицинская CRM - Панель управления",
        activePage = "dashboard",
        contentView = "pages/content/dashboard",
        pageScript = "dashboard-app.js",
        extra = mapOf("todayAppointments" to mockAppointments())
    )

    @Get("/login")
    fun login(): ModelAndView<Map<String, Any>> = appPage(
        title = "Вход в систему - Медицинская CRM",
        activePage = "login",
        contentView = "pages/content/login",
        pageScript = "login-app.js",
        authenticated = false,
        username = "Гость",
        showSidebar = false
    )

    @Get("/patients")
    fun patients(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Медицинская CRM - Пациенты",
        activePage = "patients",
        heading = "Пациенты",
        description = "Список пациентов, фильтрация и быстрый переход в карточку пациента.",
        mountId = "patients-app",
        pageScript = "patients-app.js"
    )

    @Get("/patients/{id}")
    fun patientDetail(id: Long): ModelAndView<Map<String, Any>> = genericPage(
        title = "Карточка пациента - Медицинская CRM",
        activePage = "patients",
        heading = "Карточка пациента #$id",
        description = "Подробные данные пациента, история посещений и назначения.",
        mountId = "patient-detail-app",
        pageScript = "patient-detail-app.js"
    )

    @Get("/doctors")
    fun doctors(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Медицинская CRM - Врачи",
        activePage = "doctors",
        heading = "Врачи",
        description = "Управление карточками врачей и доступными слотами для записи.",
        mountId = "doctors-app",
        pageScript = "doctors-app.js"
    )

    @Get("/schedule")
    fun schedule(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Медицинская CRM - Расписание",
        activePage = "schedule",
        heading = "Расписание",
        description = "Календарь приемов и управление расписанием специалистов.",
        mountId = "schedule-app",
        pageScript = "schedule-app.js"
    )

    @Get("/reports")
    fun reports(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Медицинская CRM - Отчеты",
        activePage = "reports",
        heading = "Отчеты",
        description = "Формирование управленческих и финансовых отчетов по работе клиники.",
        mountId = "reports-app",
        pageScript = "reports-app.js"
    )

    @Get("/inventory")
    fun inventory(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Медицинская CRM - Склад",
        activePage = "inventory",
        heading = "Склад",
        description = "Контроль остатков, расхода материалов и уведомления о дефиците.",
        mountId = "inventory-app",
        pageScript = "inventory-app.js"
    )

    @Get("/audit")
    fun audit(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Медицинская CRM - Логи действий",
        activePage = "audit",
        heading = "Аудит",
        description = "Журнал действий пользователей и технических операций в системе.",
        mountId = "audit-app",
        pageScript = "audit-app.js"
    )

    @Get("/settings")
    fun settings(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Медицинская CRM - Настройки",
        activePage = "settings",
        heading = "Настройки",
        description = "Управление параметрами системы, ролями и конфигурацией окружения.",
        mountId = "settings-app",
        pageScript = "settings-app.js"
    )

    @Get("/appointments")
    fun appointments(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Медицинская CRM - Записи на услуги",
        activePage = "appointments",
        heading = "Записи на услуги",
        description = "Создание и редактирование записей пациентов на медицинские услуги.",
        mountId = "appointment-new-app",
        pageScript = "appointment-new-app.js"
    )

    @Get("/appointments/{id}")
    fun appointmentDetail(id: Long): ModelAndView<Map<String, Any>> = genericPage(
        title = "Карточка записи - Медицинская CRM",
        activePage = "appointments",
        heading = "Карточка записи #$id",
        description = "Просмотр деталей записи, статуса и истории изменений.",
        mountId = "appointment-detail-app",
        pageScript = "appointment-detail-app.js"
    )

    @Get("/patient-booking")
    fun patientBooking(): ModelAndView<Map<String, Any>> = genericPage(
        title = "Онлайн-запись к врачу",
        activePage = "appointments",
        heading = "Онлайн-запись",
        description = "Публичная форма записи пациента к врачу.",
        mountId = "patient-booking-app",
        pageScript = "patient-booking.js",
        authenticated = false,
        username = "Гость",
        showSidebar = false
    )

    @Get("/{legacyPage}.html")
    fun legacyRoutes(
        legacyPage: String,
        @QueryValue("id") id: Long?
    ): HttpResponse<Any> {
        val target = when (legacyPage) {
            "index" -> "/"
            "login" -> "/login"
            "patients" -> "/patients"
            "patient-detail" -> if (id != null) "/patients/$id" else "/patients"
            "doctors" -> "/doctors"
            "schedule" -> "/schedule"
            "reports" -> "/reports"
            "inventory" -> "/inventory"
            "audit" -> "/audit"
            "settings" -> "/settings"
            "appointment" -> "/appointments"
            "appointment-detail" -> if (id != null) "/appointments/$id" else "/appointments"
            "patient-booking" -> "/patient-booking"
            else -> "/"
        }
        return HttpResponse.redirect(URI.create(target))
    }

    private fun genericPage(
        title: String,
        activePage: String,
        heading: String,
        description: String,
        mountId: String,
        pageScript: String,
        authenticated: Boolean = true,
        username: String = "Иван Сидоров",
        showSidebar: Boolean = true
    ): ModelAndView<Map<String, Any>> = appPage(
        title = title,
        activePage = activePage,
        contentView = "pages/content/generic",
        pageScript = pageScript,
        authenticated = authenticated,
        username = username,
        showSidebar = showSidebar,
        extra = mapOf(
            "pageHeading" to heading,
            "pageDescription" to description,
            "mountId" to mountId
        )
    )

    private fun appPage(
        title: String,
        activePage: String,
        contentView: String,
        contentFragment: String = "content",
        pageScript: String? = null,
        authenticated: Boolean = true,
        username: String = "Иван Сидоров",
        showHeader: Boolean = true,
        showFooter: Boolean = true,
        showSidebar: Boolean = true,
        extra: Map<String, Any> = emptyMap()
    ): ModelAndView<Map<String, Any>> {
        val model = mutableMapOf<String, Any>(
            "title" to title,
            "activePage" to activePage,
            "contentView" to contentView,
            "contentFragment" to contentFragment,
            "authenticated" to authenticated,
            "username" to username,
            "showHeader" to showHeader,
            "showFooter" to showFooter,
            "showSidebar" to showSidebar,
            "currentYear" to 2026,
            "lastUpdated" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        )

        if (pageScript != null) {
            model["pageScript"] = pageScript
        }
        model.putAll(extra)

        return ModelAndView("pages/app", model)
    }

    private fun mockAppointments(): List<AppointmentCard> = listOf(
        AppointmentCard("09:00", "Иванов И.И.", "/appointments/101", false),
        AppointmentCard("10:30", "Петрова А.С.", "/appointments/102", true),
        AppointmentCard("12:15", "Смирнов К.Д.", "/appointments/103", false)
    )

    private data class AppointmentCard(
        val time: String,
        val patient: String,
        val link: String,
        val isNew: Boolean
    )
}
