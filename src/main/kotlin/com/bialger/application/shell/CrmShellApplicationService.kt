package com.bialger.application.shell

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.clinical.mvc.MedicalRecordMvcService
import com.bialger.domain.clinical.repository.MedicalRecordRepository
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeBranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.core.repository.SpecialtyRepository
import com.bialger.domain.finance.entity.PaymentEntity
import com.bialger.domain.finance.enums.PaymentStatusType
import com.bialger.domain.finance.repository.PaymentRepository
import com.bialger.domain.inventory.mvc.InventoryItemMvcService
import com.bialger.domain.inventory.repository.InventoryCategoryRepository
import com.bialger.domain.patient.mvc.PatientMvcService
import com.bialger.domain.patient.repository.PatientTagRepository
import com.bialger.domain.patient.repository.PatientTagTypeRepository
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.mvc.AppointmentListRow
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import com.bialger.domain.scheduling.mvc.TimeSlotMvcService
import com.bialger.domain.scheduling.repository.ServiceRepository
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import com.bialger.domain.attachment.repository.IntegrationRepository
import com.bialger.domain.clinical.repository.TemplateRepository
import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.mvc.SystemSettingMvcService
import com.bialger.domain.system.repository.AuditLogRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Application service: builds views for SPA/Thymeleaf and REST bootstrap.
 * No transport logic here (controllers stay thin).
 *
 * Read-only transaction: JDBC repositories (в т.ч. [EmployeeBranchRepository]) требуют активное соединение.
 */
@Singleton
@Transactional(readOnly = true)
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
    private val auditLogRepository: AuditLogRepository,
    private val medicalRecordMvcService: MedicalRecordMvcService,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val roleRepository: RoleRepository,
    private val specialtyRepository: SpecialtyRepository,
    private val patientTagRepository: PatientTagRepository,
    private val patientTagTypeRepository: PatientTagTypeRepository,
    private val paymentRepository: PaymentRepository,
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val serviceRepository: ServiceRepository,
    private val templateRepository: TemplateRepository,
    private val integrationRepository: IntegrationRepository,
    private val inventoryCategoryRepository: InventoryCategoryRepository
) {

    fun patientsPayload(): Map<String, Any?> {
        val patients = patientMvcService.listAll()
        val iconsByPatient = loadPatientIcons(patients.map { it.id })
        return mapOf(
            "kind" to "patients",
            "apiBase" to "/api/patients",
            "items" to patients.map { p ->
                patientVm(p, iconsByPatient[p.id].orEmpty())
            },
            "organizations" to organizationRepository.findAllOrdered().map { o ->
                mapOf("id" to o.id.toString(), "name" to o.name)
            }
        )
    }

    fun patientDetailPayload(id: UUID): Map<String, Any?>? {
        val p = patientMvcService.getById(id) ?: return null
        val orgName = organizationRepository.findById(p.organizationId).map { it.name }.orElse("")
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val appts = appointmentMvcService.listRows()
            .filter { it.appointment.patientId == p.id }
            .map { appointmentVm(it, slots) }
        val icons = loadPatientIcons(listOf(p.id))[p.id].orEmpty()
        val assignedTagTypeIds = patientTagRepository.findByPatientId(p.id).map { it.tagTypeId.toString() }
        return mapOf(
            "kind" to "patient-detail",
            "apiBase" to "/api/patients",
            "patient" to patientVm(p, icons) + mapOf(
                "organizationName" to orgName,
                "cardCreatedAt" to (p.createdAt?.toString() ?: ""),
                "assignedTagTypeIds" to assignedTagTypeIds
            ),
            "appointments" to appts,
            "medicalRecords" to medicalRecordMvcService.listMapsByPatientId(p.id),
            "patientTagTypes" to patientTagTypeRepository.findAllOrdered().map { t ->
                mapOf(
                    "id" to t.id.toString(),
                    "code" to t.code,
                    "name" to t.name,
                    "icon" to (t.icon ?: ""),
                    "isActive" to t.isActive
                )
            },
            "organizations" to organizationRepository.findAllOrdered().map { o ->
                mapOf("id" to o.id.toString(), "name" to o.name)
            }
        )
    }

    fun doctorsPayload(): Map<String, Any?> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val rows = appointmentMvcService.listRows()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        val sunday = monday.plusDays(6)
        val weekRows = rows.filter { r ->
            val slotDate = r.appointment.timeSlotId?.let { slots[it] }?.slotDate
                ?: r.appointment.createdAt?.atZone(zone)?.toLocalDate()
            slotDate != null && !slotDate.isBefore(monday) && !slotDate.isAfter(sunday)
        }
        return mapOf(
            "kind" to "doctors",
            "apiBase" to "/api",
            "users" to usersVm(),
            "branches" to branchesVm(),
            "appointments" to rows.map { appointmentVm(it, slots) },
            "appointmentsThisWeek" to weekRows.map { appointmentVm(it, slots) }
        )
    }

    fun schedulePayload(): Map<String, Any?> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val plist = patientMvcService.listAll()
        val iconsByPatient = loadPatientIcons(plist.map { it.id })
        return mapOf(
            "kind" to "schedule",
            "apiBase" to "/api",
            "me" to meVm(),
            "branches" to branchesVm(),
            "users" to usersVm(),
            "rooms" to roomsVm(),
            "patients" to plist.map { p -> patientVm(p, iconsByPatient[p.id].orEmpty()) },
            "appointments" to appointmentMvcService.listRows().map { appointmentVm(it, slots) },
            "timeSlots" to timeSlotMvcService.listRows().map { slotVm(it) }
        )
    }

    fun appointmentsListPayload(): Map<String, Any?> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        return mapOf(
            "kind" to "appointments-list",
            "apiBase" to "/api",
            "rows" to appointmentMvcService.listRows().map { appointmentVm(it, slots) },
            "patients" to patientMvcService.listAll().map { p ->
                mapOf("id" to p.id.toString(), "fullName" to p.fullName)
            },
            "employees" to employeeRepository.findAllOrdered().map { e ->
                mapOf("id" to e.id.toString(), "fullName" to e.fullName)
            },
            "timeSlots" to timeSlotMvcService.listRows().map { slotVm(it) },
            "branches" to branchesVm(),
            "rooms" to roomsVm(),
            "statuses" to AppointmentStatus.entries.map { it.name },
            "sources" to AppointmentSource.entries.map { it.name }
        )
    }

    fun appointmentDetailPayload(id: UUID): Map<String, Any?>? {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val row = appointmentMvcService.listRows().find { it.appointment.id == id } ?: return null
        val patient = patientMvcService.getById(row.appointment.patientId) ?: return null
        val icons = loadPatientIcons(listOf(patient.id))[patient.id].orEmpty()
        val apptMap = appointmentVm(row, slots).toMutableMap()
        apptMap["patient"] = mapOf(
            "id" to patient.id.toString(),
            "fullName" to patient.fullName,
            "phone" to (patient.phone ?: ""),
            "icons" to icons
        )
        val payments = paymentRepository.findByAppointmentId(id)
        val mr = medicalRecordRepository.findByAppointmentId(id)
        val mrMap: Map<String, Any?>? = mr?.let {
            val base = medicalRecordMvcService.toMap(it).toMutableMap()
            base["locked"] = it.isSigned
            base["lockReason"] = if (it.isSigned) {
                "Медицинская запись подписана — редактирование ограничено"
            } else {
                null
            }
            base.toMap()
        }
        val statusHistory = auditLogRepository.findByEntityTypeAndEntityId("APPOINTMENT", id).map { e ->
            mapOf(
                "action" to e.action,
                "status" to e.action,
                "changedAt" to e.timestamp.toString(),
                "changedBy" to e.employeeId.toString(),
                "diff" to (e.newValue ?: e.oldValue ?: "")
            )
        }
        return mapOf(
            "kind" to "appointment-detail",
            "apiBase" to "/api/appointments",
            "appointment" to apptMap,
            "patients" to patientMvcService.listAll().map { p ->
                mapOf("id" to p.id.toString(), "fullName" to p.fullName)
            },
            "employees" to usersVm(),
            "branches" to branchesVm(),
            "rooms" to roomsVm(),
            "timeSlots" to timeSlotMvcService.listRows().map { slotVm(it) },
            "statuses" to AppointmentStatus.entries.map { it.name },
            "sources" to AppointmentSource.entries.map { it.name },
            "payments" to payments.map { paymentEntityVm(it) },
            "payment" to payments.firstOrNull()?.let { paymentEntityVm(it) },
            "medicalRecord" to mrMap,
            "statusHistory" to statusHistory,
            "me" to meVm(),
            "permissions" to systemSettingMvcService.resolvePermissions()
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
        "items" to inventoryItemMaps(),
        "categories" to inventoryCategoryRepository.findAllOrdered().map { c ->
            mapOf("id" to c.id.toString(), "name" to c.name)
        },
        "permissions" to systemSettingMvcService.resolvePermissions()
    )

    fun settingsPayload(): Map<String, Any?> = mapOf(
        "kind" to "settings",
        "apiBase" to "/api",
        "me" to meVm(),
        "branches" to branchesVm(),
        "organizations" to organizationRepository.findAllOrdered().map { o ->
            mapOf("id" to o.id.toString(), "name" to o.name)
        },
        "employees" to usersVm(),
        "rooms" to roomsVm(),
        "roles" to roleRepository.findAllOrdered().map { r ->
            mapOf(
                "id" to r.id.toString(),
                "name" to (r.displayName ?: r.name),
                "code" to r.name
            )
        },
        "specialties" to specialtyRepository.findAllOrdered().map { s ->
            mapOf("id" to s.id.toString(), "name" to s.name)
        },
        "sections" to settingsSectionsVm(),
        "catalog" to catalogPayload(),
        "systemSettings" to systemSettingMvcService.listAll().map { s ->
            mapOf(
                "id" to s.id.toString(),
                "branchId" to (s.branchId?.toString() ?: ""),
                "key" to s.key,
                "value" to (s.value ?: ""),
                "description" to (s.description ?: "")
            )
        },
        "permissions" to systemSettingMvcService.resolvePermissions()
    )

    /** Справочник разделов настроек (вкладки как в статическом фронтенде). */
    private fun settingsSectionsVm(): List<Map<String, String>> = listOf(
        mapOf("id" to "employees", "label" to "Пользователи", "apiPath" to "/api/employees", "tab" to "org"),
        mapOf("id" to "branches", "label" to "Филиалы", "apiPath" to "/api/branches", "tab" to "org"),
        mapOf("id" to "rooms", "label" to "Кабинеты", "apiPath" to "/api/rooms", "tab" to "org"),
        mapOf("id" to "services", "label" to "Услуги", "apiPath" to "/api/catalog/services", "tab" to "clinical"),
        mapOf("id" to "templates", "label" to "Шаблоны", "apiPath" to "/api/catalog/templates", "tab" to "clinical"),
        mapOf("id" to "patient-tag-types", "label" to "Значки пациентов", "apiPath" to "/api/catalog/patient-tag-types", "tab" to "clinical"),
        mapOf("id" to "integrations", "label" to "Интеграции", "apiPath" to "/api/catalog/integrations", "tab" to "org"),
        mapOf("id" to "time-slots", "label" to "Слоты расписания", "apiPath" to "/api/time-slots", "tab" to "clinical"),
        mapOf("id" to "patients", "label" to "Пациенты", "apiPath" to "/api/patients", "tab" to "clinical"),
        mapOf("id" to "appointments", "label" to "Записи", "apiPath" to "/api/appointments", "tab" to "clinical"),
        mapOf("id" to "inventory", "label" to "Склад (ТМЦ)", "apiPath" to "/api/inventory-items", "tab" to "finance"),
        mapOf("id" to "audit", "label" to "Журнал аудита", "apiPath" to "/api/audit-logs", "tab" to "finance"),
        mapOf("id" to "reports-summary", "label" to "Сводка отчётов", "apiPath" to "/api/reports/summary", "tab" to "finance")
    )

    fun reportsPayload(): Map<String, Any?> {
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val rows = appointmentMvcService.listRows()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayCount = rows.count { r ->
            val a = r.appointment
            val t = a.timeSlotId?.let { slots[it] }
            if (t != null) t.slotDate == today
            else a.createdAt?.let { ins ->
                ins.atZone(zone).toLocalDate() == today
            } == true
        }
        val payments = paymentRepository.findAllOrdered()
        val stats = buildReportsStats(rows, slots, payments)
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
            "stats" to stats,
            "apiResourceLinks" to listOf(
                mapOf("label" to "Пациенты", "path" to "/api/patients"),
                mapOf("label" to "Записи", "path" to "/api/appointments"),
                mapOf("label" to "Слоты расписания", "path" to "/api/time-slots"),
                mapOf("label" to "Сотрудники", "path" to "/api/employees"),
                mapOf("label" to "Филиалы", "path" to "/api/branches"),
                mapOf("label" to "Кабинеты", "path" to "/api/rooms"),
                mapOf("label" to "Склад (ТМЦ)", "path" to "/api/inventory-items"),
                mapOf("label" to "Журнал аудита", "path" to "/api/audit-logs"),
                mapOf("label" to "Платежи", "path" to "/api/payments"),
                mapOf("label" to "Каталог услуг", "path" to "/api/catalog/services"),
                mapOf("label" to "Сводка (JSON)", "path" to "/api/reports/summary"),
                mapOf("label" to "Системные настройки", "path" to "/api/system-settings")
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
     * Nested collection: patient appointments.
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

    /** For REST controllers: same JSON shape as list items. */
    fun timeSlotToMap(row: com.bialger.domain.scheduling.mvc.TimeSlotListRow): Map<String, Any?> = slotVm(row)

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
                "organizationId" to b.organizationId.toString(),
                "name" to b.name,
                "address" to b.address,
                "phone" to b.phone,
                "startTime" to "08:00",
                "endTime" to "20:00"
            )
        }

    private fun usersVm(): List<Map<String, Any?>> {
        val allBranchIds = branchRepository.findAllOrdered().map { it.id.toString() }
        return employeeRepository.findAllOrdered().map { e ->
            val scoped = employeeBranchRepository.findByEmployeeId(e.id).map { it.branchId.toString() }
            val branchScope = if (scoped.isNotEmpty()) scoped else allBranchIds
            val roleId = employeeRoleRepository.findByEmployeeId(e.id).firstOrNull()?.roleId
            val roleEnt = roleId?.let { roleRepository.findById(it).orElse(null) }
            val roleLabel = roleEnt?.displayName ?: roleEnt?.name ?: "Сотрудник"
            val roleCode = roleEnt?.name ?: "STAFF"
            mapOf(
                "id" to e.id.toString(),
                "login" to (e.email ?: e.id.toString().take(8)),
                "name" to e.fullName,
                "role" to roleLabel,
                "roleCode" to roleCode,
                "isActive" to e.isActive,
                "branchScope" to branchScope
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
            "branchName" to row.branchName,
            "isAvailable" to s.isAvailable
        )
    }

    private fun patientVm(
        p: com.bialger.domain.patient.entity.PatientEntity,
        iconStrings: List<String> = emptyList()
    ): Map<String, Any?> =
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
            "icons" to iconStrings
        )

    private fun appointmentVm(row: AppointmentListRow, slots: Map<UUID, TimeSlotEntity>): Map<String, Any?> {
        val a = row.appointment
        val start = resolveStart(a, slots)
        val end = resolveEnd(a, slots)
        return mapOf(
            "id" to a.id.toString(),
            "patientId" to a.patientId.toString(),
            "patientName" to row.patientName,
            "employeeName" to row.employeeName,
            "slotLabel" to row.slotLabel,
            "patient" to mapOf("id" to a.patientId.toString(), "fullName" to row.patientName),
            "doctorId" to a.employeeId.toString(),
            "employeeId" to a.employeeId.toString(),
            "branchId" to a.branchId.toString(),
            "roomId" to a.roomId.toString(),
            "status" to mapStatusToFrontend(a.status),
            "statusApi" to a.status.name,
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

    private fun loadPatientIcons(patientIds: List<UUID>): Map<UUID, List<String>> {
        if (patientIds.isEmpty()) return emptyMap()
        val tags = patientTagRepository.findByPatientIdIn(patientIds)
        val types = patientTagTypeRepository.findAllOrdered().associateBy { it.id }
        return tags.groupBy({ it.patientId }, { tag ->
            types[tag.tagTypeId]?.icon?.trim()?.takeIf { it.isNotEmpty() } ?: ""
        }).mapValues { (_, icons) -> icons.filter { it.isNotEmpty() } }
    }

    private fun paymentEntityVm(p: PaymentEntity): Map<String, Any?> {
        val total = p.amount.toDouble()
        val paid = when (p.paymentStatus) {
            PaymentStatusType.PAID -> total
            PaymentStatusType.PARTIAL -> total * 0.5
            else -> 0.0
        }
        return mapOf(
            "id" to p.id.toString(),
            "appointmentId" to p.appointmentId.toString(),
            "total" to total,
            "paid" to paid,
            "paymentMethod" to p.paymentMethod.name,
            "paymentStatus" to p.paymentStatus.name,
            "method" to p.paymentMethod.name,
            "amount" to p.amount.toPlainString(),
            "notes" to (p.notes ?: ""),
            "createdBy" to p.createdBy.toString()
        )
    }

    private fun catalogPayload(): Map<String, Any?> = mapOf(
        "services" to serviceRepository.findAllOrdered().map { s ->
            mapOf(
                "id" to s.id.toString(),
                "name" to s.name,
                "price" to s.price.toPlainString(),
                "branchId" to s.branchId?.toString(),
                "isActive" to s.isActive
            )
        },
        "templates" to templateRepository.findAllOrdered().map { t ->
            mapOf(
                "id" to t.id.toString(),
                "name" to t.name,
                "type" to t.type.name,
                "isActive" to t.isActive
            )
        },
        "integrations" to integrationRepository.findAllOrdered().map { i ->
            mapOf(
                "id" to i.id.toString(),
                "name" to i.name,
                "type" to i.type.name,
                "isActive" to i.isActive
            )
        },
        "patientTagTypes" to patientTagTypeRepository.findAllOrdered().map { p ->
            mapOf(
                "id" to p.id.toString(),
                "code" to p.code,
                "name" to p.name,
                "icon" to (p.icon ?: ""),
                "isActive" to p.isActive
            )
        }
    )

    private fun buildReportsStats(
        rows: List<AppointmentListRow>,
        slots: Map<UUID, TimeSlotEntity>,
        payments: List<PaymentEntity>
    ): Map<String, Any?> {
        val total = rows.size
        fun cnt(st: AppointmentStatus) = rows.count { it.appointment.status == st }
        val booked = cnt(AppointmentStatus.SCHEDULED)
        val confirmed = cnt(AppointmentStatus.CONFIRMED) + cnt(AppointmentStatus.ARRIVED)
        val canceled = cnt(AppointmentStatus.CANCELLED)
        val noShow = cnt(AppointmentStatus.NO_SHOW)
        val online = rows.count { it.appointment.source == AppointmentSource.ONLINE }
        val frontDesk = rows.count { it.appointment.source == AppointmentSource.MANUAL }

        val durationsMin = rows.mapNotNull { row ->
            val t = row.appointment.timeSlotId?.let { slots[it] } ?: return@mapNotNull null
            ChronoUnit.MINUTES.between(
                t.slotDate.atTime(t.startTime),
                t.slotDate.atTime(t.endTime)
            ).toInt()
        }
        val avgMin = if (durationsMin.isEmpty()) 0 else durationsMin.average().roundToInt()

        val cancelRate = if (total > 0) (canceled * 100.0 / total) else 0.0
        val noShowRate = if (total > 0) (noShow * 100.0 / total) else 0.0

        val totalRevenue = payments.fold(java.math.BigDecimal.ZERO) { a, b -> a.add(b.amount) }
        val paidRevenue = payments.filter { it.paymentStatus == PaymentStatusType.PAID }
            .fold(java.math.BigDecimal.ZERO) { a, b -> a.add(b.amount) }

        val slotCount = slots.size
        val scheduleLoad = if (slotCount > 0) {
            min(100, (total * 100.0 / slotCount).roundToInt())
        } else {
            0
        }

        return mapOf(
            "totalPatients" to patientMvcService.listAll().size,
            "totalAppointments" to total,
            "totalDoctors" to employeeRepository.findAllOrdered().count { it.isActive },
            "onlineAppointments" to online,
            "frontDeskAppointments" to frontDesk,
            "avgAppointmentTime" to avgMin,
            "cancelRate" to ((cancelRate * 10).roundToInt() / 10.0),
            "noShowRate" to ((noShowRate * 10).roundToInt() / 10.0),
            "satisfactionRate" to null,
            "scheduleLoad" to scheduleLoad,
            "bookedCount" to booked,
            "confirmedCount" to confirmed,
            "canceledCount" to canceled,
            "noShowCount" to noShow,
            "totalRevenue" to totalRevenue.toDouble(),
            "paidRevenue" to paidRevenue.toDouble()
        )
    }
}
