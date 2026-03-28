package com.bialger.application.shell

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.inventory.mvc.InventoryItemMvcService
import com.bialger.domain.patient.mvc.PatientMvcService
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.mvc.AppointmentListRow
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import com.bialger.domain.scheduling.mvc.TimeSlotMvcService
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.mvc.SystemSettingMvcService
import com.bialger.domain.system.repository.AuditLogRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import jakarta.inject.Singleton
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Application service: собирает представления для SPA/Thymeleaf и REST bootstrap,
 * не содержит транспортной логики (контроллеры остаются тонкими).
 */
@Singleton
class CrmShellApplicationService(
    private val patientMvcService: PatientMvcService,
    private val organizationRepository: OrganizationRepository,
    private val appointmentMvcService: AppointmentMvcService,
    private val timeSlotRepository: TimeSlotRepository,
    private val timeSlotMvcService: TimeSlotMvcService,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val inventoryItemMvcService: InventoryItemMvcService,
    private val systemSettingMvcService: SystemSettingMvcService,
    private val auditLogRepository: AuditLogRepository
) {

    fun patientsPayload(): Map<String, Any?> = mapOf(
        "kind" to "patients",
        "apiBase" to "/api/patients",
        "items" to patientMvcService.listAll().map { patientVm(it) }
    )

    fun patientDetailPayload(id: UUID): Map<String, Any?>? {
        val p = patientMvcService.getById(id) ?: return null
        val orgName = organizationRepository.findById(p.organizationId).map { it.name }.orElse("")
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val appts = appointmentMvcService.listRows()
            .filter { it.appointment.patientId == p.id }
            .map { appointmentVm(it, slots) }
        return mapOf(
            "kind" to "patient-detail",
            "apiBase" to "/api/patients",
            "patient" to patientVm(p) + mapOf("organizationName" to orgName),
            "appointments" to appts,
            "organizations" to organizationRepository.findAllOrdered().map { o ->
                mapOf("id" to o.id.toString(), "name" to o.name)
            }
        )
    }

    fun doctorsPayload(): Map<String, Any?> {
        val branchIds = branchRepository.findAllOrdered().map { it.id.toString() }
        val users = employeeRepository.findAllOrdered().map { e ->
            mapOf(
                "id" to e.id.toString(),
                "login" to (e.email ?: e.id.toString().take(8)),
                "name" to e.fullName,
                "role" to "DOCTOR",
                "branchScope" to branchIds
            )
        }
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        return mapOf(
            "kind" to "doctors",
            "apiBase" to "/api",
            "users" to users,
            "branches" to branchesVm(),
            "appointments" to appointmentMvcService.listRows().map { appointmentVm(it, slots) }
        )
    }

    fun schedulePayload(): Map<String, Any?> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        return mapOf(
            "kind" to "schedule",
            "apiBase" to "/api",
            "me" to meVm(),
            "branches" to branchesVm(),
            "users" to usersVm(),
            "rooms" to roomsVm(),
            "patients" to patientMvcService.listAll().map { patientVm(it) },
            "appointments" to appointmentMvcService.listRows().map { appointmentVm(it, slots) },
            "timeSlots" to timeSlotMvcService.listRows().map { slotVm(it) }
        )
    }

    fun appointmentsListPayload(): Map<String, Any?> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        return mapOf(
            "kind" to "appointments-list",
            "apiBase" to "/api/appointments",
            "rows" to appointmentMvcService.listRows().map { appointmentVm(it, slots) }
        )
    }

    fun appointmentDetailPayload(id: UUID): Map<String, Any?>? {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val row = appointmentMvcService.listRows().find { it.appointment.id == id } ?: return null
        return mapOf(
            "kind" to "appointment-detail",
            "apiBase" to "/api/appointments",
            "appointment" to appointmentVm(row, slots)
        )
    }

    fun inventoryItemMaps(): List<Map<String, Any?>> =
        inventoryItemMvcService.listRows().map { row ->
            mapOf(
                "id" to row.item.id.toString(),
                "name" to row.item.name,
                "branchId" to row.item.branchId.toString(),
                "quantity" to row.item.quantity,
                "unit" to row.item.unit,
                "minQuantity" to row.item.minQuantity,
                "categoryName" to row.categoryName,
                "roomName" to (row.roomName ?: "")
            )
        }

    fun inventoryPayload(): Map<String, Any?> = mapOf(
        "kind" to "inventory",
        "me" to meVm(),
        "branches" to branchesVm(),
        "users" to usersVm(),
        "apiBase" to "/api/inventory-items",
        "items" to inventoryItemMaps()
    )

    fun settingsPayload(): Map<String, Any?> = mapOf(
        "kind" to "settings",
        "apiBase" to "/api",
        "me" to meVm(),
        "branches" to branchesVm(),
        "sections" to settingsSectionsVm(),
        "systemSettings" to systemSettingMvcService.listAll().map { s ->
            mapOf(
                "id" to s.id.toString(),
                "branchId" to (s.branchId?.toString() ?: ""),
                "key" to s.key,
                "value" to (s.value ?: ""),
                "description" to (s.description ?: "")
            )
        }
    )

    private fun settingsSectionsVm(): List<Map<String, String>> = listOf(
        mapOf("id" to "organizations", "label" to "Организации", "href" to "/mvc/organizations", "tab" to "org"),
        mapOf("id" to "branches", "label" to "Филиалы", "href" to "/mvc/branches", "tab" to "org"),
        mapOf("id" to "rooms", "label" to "Кабинеты", "href" to "/mvc/rooms", "tab" to "org"),
        mapOf("id" to "roles", "label" to "Роли", "href" to "/mvc/roles", "tab" to "org"),
        mapOf("id" to "permissions", "label" to "Права доступа", "href" to "/mvc/permissions", "tab" to "org"),
        mapOf("id" to "employees", "label" to "Сотрудники", "href" to "/mvc/employees", "tab" to "org"),
        mapOf("id" to "specialties", "label" to "Специализации", "href" to "/mvc/specialties", "tab" to "org"),
        mapOf("id" to "patient-tag-types", "label" to "Типы тегов пациента", "href" to "/mvc/patient-tag-types", "tab" to "clinical"),
        mapOf("id" to "templates", "label" to "Шаблоны документов", "href" to "/mvc/templates", "tab" to "clinical"),
        mapOf("id" to "laboratories", "label" to "Лаборатории", "href" to "/mvc/laboratories", "tab" to "clinical"),
        mapOf("id" to "lab-tests", "label" to "Лабораторные тесты", "href" to "/mvc/lab-tests", "tab" to "clinical"),
        mapOf("id" to "salary-records", "label" to "Записи зарплаты", "href" to "/mvc/salary-records", "tab" to "finance"),
        mapOf("id" to "payments", "label" to "Платежи", "href" to "/mvc/payments", "tab" to "finance"),
        mapOf("id" to "integrations", "label" to "Интеграции", "href" to "/mvc/integrations", "tab" to "finance")
    )

    fun reportsPayload(): Map<String, Any?> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val rows = appointmentMvcService.listRows()
        val today = java.time.LocalDate.now(ZoneId.systemDefault())
        val todayCount = rows.count { r ->
            val a = r.appointment
            val t = a.timeSlotId?.let { slots[it] }
            if (t != null) t.slotDate == today
            else a.createdAt?.let { ins ->
                ins.atZone(ZoneId.systemDefault()).toLocalDate() == today
            } == true
        }
        return mapOf(
            "kind" to "reports",
            "apiBase" to "/api",
            "summary" to mapOf(
                "patientsTotal" to patientMvcService.listAll().size,
                "appointmentsTotal" to rows.size,
                "appointmentsToday" to todayCount,
                "employeesTotal" to employeeRepository.findAllOrdered().size,
                "branchesTotal" to branchRepository.findAllOrdered().size
            ),
            "mvcLinks" to listOf(
                mapOf("label" to "Пациенты", "href" to "/mvc/patients"),
                mapOf("label" to "Записи", "href" to "/mvc/appointments"),
                mapOf("label" to "Расписание (слоты)", "href" to "/mvc/time-slots"),
                mapOf("label" to "Сотрудники", "href" to "/mvc/employees"),
                mapOf("label" to "Филиалы", "href" to "/mvc/branches"),
                mapOf("label" to "Склад (ТМЦ)", "href" to "/mvc/inventory-items"),
                mapOf("label" to "Платежи", "href" to "/mvc/payments"),
                mapOf("label" to "Услуги", "href" to "/mvc/medical-services")
            )
        )
    }

    fun auditPage(pageable: Pageable): Page<Map<String, Any?>> {
        val page = auditLogRepository.findRecent(pageable)
        val employees = employeeRepository.findAllOrdered().associateBy { it.id }
        val maps = page.content.map { auditVm(it, employees) }
        return Page.of(maps, page.pageable, page.totalSize)
    }

    fun auditPayload(): Map<String, Any?> {
        val page = auditLogRepository.findRecent(Pageable.from(0, 200))
        val employees = employeeRepository.findAllOrdered().associateBy { it.id }
        val logs = page.content.map { e -> auditVm(e, employees) }
        return mapOf(
            "kind" to "audit",
            "apiBase" to "/api",
            "logs" to logs
        )
    }

    fun dashboardPayload(): Map<String, Any?> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val rows = appointmentMvcService.listRows().map { appointmentVm(it, slots) }
        return mapOf(
            "kind" to "dashboard",
            "apiBase" to "/api",
            "me" to meVm(),
            "appointments" to rows
        )
    }

    /**
     * Вложенная коллекция: записи пациента.
     */
    fun appointmentMapsForPatient(patientId: UUID): List<Map<String, Any?>> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        return appointmentMvcService.listRows()
            .filter { it.appointment.patientId == patientId }
            .map { appointmentVm(it, slots) }
    }

    fun listAppointmentMaps(): List<Map<String, Any?>> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        return appointmentMvcService.listRows().map { appointmentVm(it, slots) }
    }

    fun appointmentMapById(id: UUID): Map<String, Any?>? {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val row = appointmentMvcService.listRows().find { it.appointment.id == id } ?: return null
        return appointmentVm(row, slots)
    }

    fun branchesList(): List<Map<String, Any?>> = branchesVm()

    fun roomsList(): List<Map<String, Any?>> = roomsVm()

    fun employeesList(): List<Map<String, Any?>> = usersVm()

    fun timeSlotsList(): List<Map<String, Any?>> = timeSlotMvcService.listRows().map { slotVm(it) }

    fun mePayload(): Map<String, Any?> = meVm()

    fun bootstrapPayload(kind: String, patientId: UUID?, appointmentId: UUID?): Map<String, Any?> =
        when (kind) {
            "dashboard" -> dashboardPayload()
            "patients" -> patientsPayload()
            "patient-detail" -> {
                val id = patientId ?: throw IllegalArgumentException("patientId required")
                patientDetailPayload(id)
                    ?: mapOf("kind" to "patient-detail", "error" to "not_found")
            }
            "doctors" -> doctorsPayload()
            "schedule" -> schedulePayload()
            "inventory" -> inventoryPayload()
            "settings" -> settingsPayload()
            "appointments-list" -> appointmentsListPayload()
            "appointment-detail" -> {
                val id = appointmentId ?: throw IllegalArgumentException("appointmentId required")
                appointmentDetailPayload(id)
                    ?: mapOf("kind" to "appointment-detail", "error" to "not_found")
            }
            "reports" -> reportsPayload()
            "audit" -> auditPayload()
            else -> throw IllegalArgumentException("unknown kind: $kind")
        }

    private fun auditVm(
        e: AuditLogEntity,
        employees: Map<UUID, EmployeeEntity>
    ): Map<String, Any?> {
        val emp = employees[e.employeeId]
        return mapOf(
            "id" to e.id.toString(),
            "employeeId" to e.employeeId.toString(),
            "userId" to e.employeeId.toString(),
            "employeeName" to (emp?.fullName ?: e.employeeId.toString()),
            "action" to e.action,
            "entityType" to e.entityType,
            "entityId" to e.entityId?.toString(),
            "ts" to e.timestamp.toString(),
            "timestamp" to e.timestamp.toString(),
            "diff" to mapOf(
                "old" to e.oldValue,
                "new" to e.newValue
            ),
            "branchId" to null
        )
    }

    private fun meVm(): Map<String, Any?> {
        val u = employeeRepository.findAllOrdered().firstOrNull()
        val branches = branchRepository.findAllOrdered().map { it.id.toString() }
        val perms = systemSettingMvcService.resolvePermissions()
        return mapOf(
            "user" to mapOf(
                "id" to (u?.id?.toString() ?: "00000000-0000-0000-0000-000000000001"),
                "name" to (u?.fullName ?: "Пользователь"),
                "role" to "ADMIN",
                "login" to (u?.email ?: "admin@local")
            ),
            "branchScope" to if (branches.isNotEmpty()) branches else listOf("00000000-0000-0000-0000-000000000001"),
            "permissions" to perms
        )
    }

    private fun branchesVm(): List<Map<String, Any?>> =
        branchRepository.findAllOrdered().map { b ->
            mapOf(
                "id" to b.id.toString(),
                "name" to b.name,
                "address" to b.address,
                "phone" to b.phone,
                "startTime" to "08:00",
                "endTime" to "20:00"
            )
        }

    private fun usersVm(): List<Map<String, Any?>> {
        val branchIds = branchRepository.findAllOrdered().map { it.id.toString() }
        return employeeRepository.findAllOrdered().map { e ->
            mapOf(
                "id" to e.id.toString(),
                "login" to (e.email ?: e.id.toString().take(8)),
                "name" to e.fullName,
                "role" to "DOCTOR",
                "branchScope" to branchIds
            )
        }
    }

    private fun roomsVm(): List<Map<String, Any?>> =
        roomRepository.findAllOrdered().map { r ->
            mapOf(
                "id" to r.id.toString(),
                "branchId" to r.branchId.toString(),
                "name" to r.name,
                "number" to r.name
            )
        }

    private fun slotVm(row: com.bialger.domain.scheduling.mvc.TimeSlotListRow): Map<String, Any?> {
        val s = row.slot
        val start = s.slotDate.atTime(s.startTime).atZone(ZoneId.systemDefault()).toInstant()
        val end = s.slotDate.atTime(s.endTime).atZone(ZoneId.systemDefault()).toInstant()
        return mapOf(
            "id" to s.id.toString(),
            "employeeId" to s.employeeId.toString(),
            "branchId" to s.branchId.toString(),
            "roomId" to s.roomId.toString(),
            "slotDate" to s.slotDate.toString(),
            "start" to start.toString(),
            "end" to end.toString(),
            "startTime" to s.startTime.toString(),
            "endTime" to s.endTime.toString(),
            "employeeName" to row.employeeName,
            "roomName" to row.roomName,
            "branchName" to row.branchName
        )
    }

    private fun patientVm(p: com.bialger.domain.patient.entity.PatientEntity): Map<String, Any?> =
        mapOf(
            "id" to p.id.toString(),
            "organizationId" to p.organizationId.toString(),
            "fullName" to p.fullName,
            "phone" to p.phone,
            "dob" to p.birthDate?.toString(),
            "birthDate" to p.birthDate?.toString(),
            "gender" to p.gender?.name,
            "cardNumber" to p.cardNumber,
            "email" to p.email,
            "registrationAddress" to p.registrationAddress,
            "residenceAddress" to p.residenceAddress,
            "localityType" to p.localityType?.name,
            "citizenship" to p.citizenship,
            "identityDocument" to p.identityDocument,
            "omsPolicy" to p.omsPolicy,
            "snils" to p.snils,
            "insuranceOrganization" to p.insuranceOrganization,
            "contactPerson" to p.contactPerson,
            "guardian" to p.guardian,
            "profession" to p.profession,
            "workplace" to p.workplace,
            "icons" to emptyList<Any>()
        )

    private fun appointmentVm(row: AppointmentListRow, slots: Map<UUID, TimeSlotEntity>): Map<String, Any?> {
        val a = row.appointment
        val start = resolveStart(a, slots)
        val end = resolveEnd(a, slots)
        return mapOf(
            "id" to a.id.toString(),
            "patientId" to a.patientId.toString(),
            "patient" to mapOf("id" to a.patientId.toString(), "fullName" to row.patientName),
            "doctorId" to a.employeeId.toString(),
            "employeeId" to a.employeeId.toString(),
            "branchId" to a.branchId.toString(),
            "roomId" to a.roomId.toString(),
            "status" to mapStatusToFrontend(a.status),
            "source" to a.source.name,
            "start" to (start?.toString()),
            "end" to (end?.toString()),
            "notes" to a.notes,
            "timeSlotId" to a.timeSlotId?.toString()
        )
    }

    private fun resolveStart(a: AppointmentEntity, slots: Map<UUID, TimeSlotEntity>): Instant? {
        val t = a.timeSlotId?.let { slots[it] } ?: return a.createdAt
        return t.slotDate.atTime(t.startTime).atZone(ZoneId.systemDefault()).toInstant()
    }

    private fun resolveEnd(a: AppointmentEntity, slots: Map<UUID, TimeSlotEntity>): Instant? {
        val t = a.timeSlotId?.let { slots[it] }
        return if (t != null) {
            t.slotDate.atTime(t.endTime).atZone(ZoneId.systemDefault()).toInstant()
        } else {
            val s = a.createdAt ?: return null
            Instant.ofEpochMilli(s.toEpochMilli() + 30 * 60_000L)
        }
    }

    private fun mapStatusToFrontend(s: AppointmentStatus): String = when (s) {
        AppointmentStatus.SCHEDULED -> "BOOKED"
        AppointmentStatus.CONFIRMED -> "CONFIRMED"
        AppointmentStatus.ARRIVED -> "CONFIRMED"
        AppointmentStatus.NO_SHOW -> "CANCELLED"
        AppointmentStatus.CANCELLED -> "CANCELLED"
    }
}
