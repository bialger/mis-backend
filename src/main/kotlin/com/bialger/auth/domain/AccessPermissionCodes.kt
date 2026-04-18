package com.bialger.auth.domain

data class AccessPermissionDefinition(
    val code: String,
    val name: String,
    val description: String
)

object AccessPermissionCodes {
    const val DASHBOARD_VIEW = "dashboard.view"
    const val POSTS_VIEW = "posts.view"
    const val PATIENTS_VIEW = "patients.view"
    const val DOCTORS_VIEW = "doctors.view"
    const val SCHEDULE_VIEW = "schedule.view"
    const val APPOINTMENTS_VIEW = "appointments.view"
    const val REPORTS_VIEW = "reports.view"
    const val INVENTORY_VIEW = "inventory.view"
    const val INVENTORY_WRITE = "inventory.write"
    const val AUDIT_VIEW = "audit.view"
    const val SETTINGS_VIEW = "settings.view"
    const val SETTINGS_WRITE = "settings.write"

    const val EMPLOYEE_READ = "employee.read"
    const val EMPLOYEE_WRITE = "employee.write"
    const val BRANCH_READ = "branch.read"
    const val BRANCH_WRITE = "branch.write"
    const val ORGANIZATION_READ = "organization.read"
    const val ORGANIZATION_WRITE = "organization.write"
    const val PERMISSION_READ = "permission.read"
    const val PERMISSION_WRITE = "permission.write"

    const val FINANCE_VIEW = "finance.view"
    const val FINANCE_WRITE = "finance.write"
    const val MANUAL_EGISZ_SEND = "egisz.manual.send"
    const val APPOINTMENT_BACKDATE_EDIT = "appointments.backdate.edit"

    val ALL: List<AccessPermissionDefinition> = listOf(
        AccessPermissionDefinition(DASHBOARD_VIEW, "Просмотр панели", "Доступ к /"),
        AccessPermissionDefinition(POSTS_VIEW, "Просмотр постов", "Доступ к /posts"),
        AccessPermissionDefinition(PATIENTS_VIEW, "Пациенты", "Просмотр и работа с пациентами"),
        AccessPermissionDefinition(DOCTORS_VIEW, "Врачи", "Просмотр раздела врачей"),
        AccessPermissionDefinition(SCHEDULE_VIEW, "Расписание", "Просмотр и работа с расписанием"),
        AccessPermissionDefinition(APPOINTMENTS_VIEW, "Записи", "Просмотр и работа с записями"),
        AccessPermissionDefinition(REPORTS_VIEW, "Отчеты", "Доступ к отчетам"),
        AccessPermissionDefinition(INVENTORY_VIEW, "Склад: просмотр", "Просмотр остатков склада"),
        AccessPermissionDefinition(INVENTORY_WRITE, "Склад: изменение", "Операции изменения склада"),
        AccessPermissionDefinition(AUDIT_VIEW, "Аудит", "Просмотр журнала аудита"),
        AccessPermissionDefinition(SETTINGS_VIEW, "Настройки: просмотр", "Доступ к экрану настроек"),
        AccessPermissionDefinition(SETTINGS_WRITE, "Настройки: изменение", "Изменение системных настроек"),
        AccessPermissionDefinition(EMPLOYEE_READ, "Сотрудники: просмотр", "Просмотр сотрудников"),
        AccessPermissionDefinition(EMPLOYEE_WRITE, "Сотрудники: изменение", "Создание и редактирование сотрудников"),
        AccessPermissionDefinition(BRANCH_READ, "Филиалы: просмотр", "Просмотр филиалов"),
        AccessPermissionDefinition(BRANCH_WRITE, "Филиалы: изменение", "Создание и редактирование филиалов"),
        AccessPermissionDefinition(ORGANIZATION_READ, "Организации: просмотр", "Просмотр организаций"),
        AccessPermissionDefinition(ORGANIZATION_WRITE, "Организации: изменение", "Создание и редактирование организаций"),
        AccessPermissionDefinition(PERMISSION_READ, "Права: просмотр", "Просмотр матрицы прав"),
        AccessPermissionDefinition(PERMISSION_WRITE, "Права: изменение", "Изменение прав сотрудников"),
        AccessPermissionDefinition(FINANCE_VIEW, "Финансы: просмотр", "Просмотр финансовых данных"),
        AccessPermissionDefinition(FINANCE_WRITE, "Финансы: изменение", "Редактирование финансовых данных"),
        AccessPermissionDefinition(MANUAL_EGISZ_SEND, "ЕГИСЗ: ручная отправка", "Ручная отправка в ЕГИСЗ"),
        AccessPermissionDefinition(APPOINTMENT_BACKDATE_EDIT, "Редактирование задним числом", "Редактирование записей задним числом")
    )
}
