package com.bialger.application.shell

import com.bialger.api.dto.AppointmentRestDto
import com.bialger.api.dto.AuditDiffDto
import com.bialger.api.dto.AuditLogEntryDto
import com.bialger.api.dto.BranchRestDto
import com.bialger.api.dto.EmployeeRestDto
import com.bialger.api.dto.InventoryItemRestDto
import com.bialger.api.dto.MeRestDto
import com.bialger.auth.application.CurrentUserContextService
import com.bialger.api.dto.PatientRestDto
import com.bialger.api.dto.PaymentRestDto
import com.bialger.api.dto.ReportsApiLinkDto
import com.bialger.api.dto.ReportsStatsDto
import com.bialger.api.dto.ReportsSummaryCountsDto
import com.bialger.api.dto.RoomRestDto
import com.bialger.api.dto.SystemSettingRestDto
import com.bialger.api.dto.TimeSlotRestDto
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
import com.bialger.domain.patient.repository.PatientConsentRepository
import com.bialger.domain.patient.repository.PatientTagRepository
import com.bialger.domain.patient.repository.PatientTagTypeRepository
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.mvc.AppointmentListRow
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import com.bialger.domain.scheduling.mvc.TimeSlotListRow
import com.bialger.domain.scheduling.mvc.TimeSlotMvcService
import com.bialger.domain.scheduling.repository.ServiceRepository
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import com.bialger.domain.attachment.repository.IntegrationRepository
import com.bialger.domain.clinical.repository.TemplateRepository
import com.bialger.domain.system.entity.AuditLogEntity
import com.bialger.domain.system.mvc.SystemSettingMvcService
import com.bialger.domain.system.repository.AuditLogRepository
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
 * Read-only transaction: JDBC repositories (including [EmployeeBranchRepository]) require an active connection.
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
    private val patientConsentRepository: PatientConsentRepository,
    private val paymentRepository: PaymentRepository,
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val serviceRepository: ServiceRepository,
    private val templateRepository: TemplateRepository,
    private val integrationRepository: IntegrationRepository,
    private val inventoryCategoryRepository: InventoryCategoryRepository,
    private val currentUserContextService: CurrentUserContextService
) {

    // ── Public payload methods (used by CrmShellPageData + ShellBootstrapApiController) ──

    fun patientsPayload(): Map<String, Any?> {
        val patients = patientMvcService.listAll()
        val iconsByPatient = loadPatientIcons(patients.map { it.id })
        return mapOf(
            "kind" to "patients",
            "apiBase" to "/api/patients",
            "items" to patients.map { p -> patientVm(p, iconsByPatient[p.id].orEmpty()) },
            "organizations" to organizationRepository.findAllOrdered().map { o ->
                mapOf("id" to o.id.toString(), "name" to o.name)
            }
        )
    }

    fun patientDetailPayload(id: UUID): Map<String, Any?>? {
        val p = patientMvcService.getById(id) ?: return null
        val orgName = organizationRepository.findById(p.organizationId).map { it.name }.orElse("")
        val slots = timeSlotRepository.findAllOrdered().associateBy { it.id }
        val appts: List<AppointmentRestDto> = appointmentMvcService.listRows()
            .filter { it.appointment.patientId == p.id }
            .map { appointmentVm(it, slots) }
        val icons = loadPatientIcons(listOf(p.id))[p.id].orEmpty()
        val assignedTagTypeIds = patientTagRepository.findByPatientId(p.id).map { it.tagTypeId.toString() }
        val base = patientVm(p, icons)
        // Detail view needs extra fields beyond PatientRestDto — build a combined map
        val patientDetail: Map<String, Any?> = mapOf(
            "id" to base.id, "organizationId" to base.organizationId,
            "cardNumber" to base.cardNumber, "fullName" to base.fullName,
            "gender" to base.gender, "birthDate" to base.birthDate, "dob" to base.dob,
            "phone" to base.phone, "email" to base.email,
            "registrationAddress" to base.registrationAddress,
            "residenceAddress" to base.residenceAddress,
            "localityType" to base.localityType, "citizenship" to base.citizenship,
            "identityDocument" to base.identityDocument, "omsPolicy" to base.omsPolicy,
            "snils" to base.snils, "insuranceOrganization" to base.insuranceOrganization,
            "contactPerson" to base.contactPerson, "guardian" to base.guardian,
            "profession" to base.profession, "workplace" to base.workplace,
            "icons" to base.icons,
            "organizationName" to orgName,
            "cardCreatedAt" to (p.createdAt?.toString() ?: ""),
            "assignedTagTypeIds" to assignedTagTypeIds
        )
        return mapOf(
            "kind" to "patient-detail",
            "apiBase" to "/api/patients",
            "patient" to patientDetail,
            "appointments" to appts,
            "medicalRecords" to medicalRecordMvcService.listByPatientId(p.id),
            "me" to meVm(),
            "consents" to patientConsentRepository.findByPatientId(p.id).map { c ->
                mapOf(
                    "id" to c.id.toString(),
                    "consentType" to c.consentType.name,
                    "isGranted" to c.isGranted,
                    "grantedAt" to c.grantedAt?.toString(),
                    "revokedAt" to c.revokedAt?.toString()
                )
            },
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

        // Extend AppointmentRestDto with the detail-specific patient sub-object
        val apptDto = appointmentVm(row, slots)
        val apptMap: Map<String, Any?> = mapOf(
            "id" to apptDto.id,
            "patientId" to apptDto.patientId,
            "patientName" to apptDto.patientName,
            "employeeName" to apptDto.employeeName,
            "slotLabel" to apptDto.slotLabel,
            "doctorId" to apptDto.doctorId,
            "employeeId" to apptDto.employeeId,
            "branchId" to apptDto.branchId,
            "roomId" to apptDto.roomId,
            "status" to apptDto.status,
            "statusApi" to apptDto.statusApi,
            "source" to apptDto.source,
            "start" to apptDto.start,
            "end" to apptDto.end,
            "notes" to apptDto.notes,
            "timeSlotId" to apptDto.timeSlotId,
            "patient" to mapOf(
                "id" to patient.id.toString(),
                "fullName" to patient.fullName,
                "phone" to (patient.phone ?: ""),
                "icons" to icons
            )
        )

        val payments: List<PaymentRestDto> = paymentRepository.findByAppointmentId(id)
            .map { paymentEntityVm(it) }

        // MedicalRecord needs shell-specific `locked`/`lockReason` on top of the DTO fields
        val mr = medicalRecordRepository.findByAppointmentId(id)
        val mrMap: Map<String, Any?>? = mr?.let { rec ->
            val dto = medicalRecordMvcService.toDto(rec)
            mapOf(
                "id" to dto.id,
                "appointmentId" to dto.appointmentId,
                "patientId" to dto.patientId,
                "employeeId" to dto.employeeId,
                "complaints" to dto.complaints,
                "anamnesis" to dto.anamnesis,
                "examinationResults" to dto.examinationResults,
                "diseaseCourse" to dto.diseaseCourse,
                "procedures" to dto.procedures,
                "epicrisis" to dto.epicrisis,
                "isSigned" to dto.isSigned,
                "templateId" to dto.templateId,
                "locked" to rec.isSigned,
                "lockReason" to if (rec.isSigned) {
                    "Медицинская запись подписана — редактирование ограничено"
                } else null
            )
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
            "payments" to payments,
            "payment" to payments.firstOrNull(),
            "medicalRecord" to mrMap,
            "statusHistory" to statusHistory,
            "me" to meVm(),
            "permissions" to currentPermissions()
        )
    }

    fun inventoryItemMaps(): List<InventoryItemRestDto> =
        inventoryItemMvcService.listRows().map { row ->
            InventoryItemRestDto(
                id = row.item.id.toString(),
                name = row.item.name,
                branchId = row.item.branchId.toString(),
                quantity = row.item.quantity,
                unit = row.item.unit,
                minQuantity = row.item.minQuantity,
                categoryName = row.categoryName,
                roomName = row.roomName ?: ""
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
        "permissions" to currentPermissions()
    )

    fun settingsPayload(): Map<String, Any?> = mapOf(
        "kind" to "settings",
        "apiBase" to "/api",
        "me" to meVm(),
        "branches" to branchesVm(),
        "organizations" to organizationRepository.findAllOrdered().map { o ->
            mapOf(
                "id" to o.id.toString(),
                "name" to o.name,
                "codeOkpo" to (o.codeOkpo ?: ""),
                "codeOkud" to (o.codeOkud ?: ""),
                "address" to (o.address ?: "")
            )
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
        "sections" to settingsSectionsVm(currentPermissions()),
        "catalog" to catalogPayload(),
        "systemSettings" to systemSettingMvcService.listAll().map { s ->
            SystemSettingRestDto(
                id = s.id.toString(),
                branchId = s.branchId?.toString() ?: "",
                key = s.key,
                value = s.value ?: "",
                description = s.description ?: ""
            )
        },
        "permissions" to currentPermissions()
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
            else a.createdAt?.let { ins -> ins.atZone(zone).toLocalDate() == today } == true
        }
        val payments = paymentRepository.findAllOrdered()
        val stats: ReportsStatsDto = buildReportsStats(rows, slots, payments)
        val summary = ReportsSummaryCountsDto(
            patientsTotal = patientMvcService.listAll().size,
            appointmentsTotal = rows.size,
            appointmentsToday = todayCount,
            employeesTotal = employeeRepository.findAllOrdered().size,
            branchesTotal = branchRepository.findAllOrdered().size
        )
        val links = listOf(
            ReportsApiLinkDto("Пациенты", "/api/patients"),
            ReportsApiLinkDto("Записи", "/api/appointments"),
            ReportsApiLinkDto("Слоты расписания", "/api/time-slots"),
            ReportsApiLinkDto("Сотрудники", "/api/employees"),
            ReportsApiLinkDto("Филиалы", "/api/branches"),
            ReportsApiLinkDto("Кабинеты", "/api/rooms"),
            ReportsApiLinkDto("Склад (ТМЦ)", "/api/inventory-items"),
            ReportsApiLinkDto("Журнал аудита", "/api/audit-logs"),
            ReportsApiLinkDto("Платежи", "/api/payments"),
            ReportsApiLinkDto("Каталог услуг", "/api/catalog/services"),
            ReportsApiLinkDto("Сводка (JSON)", "/api/reports/summary"),
            ReportsApiLinkDto("Системные настройки", "/api/system-settings")
        )
        return mapOf(
            "kind" to "reports",
            "apiBase" to "/api",
            "summary" to summary,
            "stats" to stats,
            "apiResourceLinks" to links
        )
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
        val appts: List<AppointmentRestDto> = appointmentMvcService.listRows()
            .map { appointmentVm(it, slots) }
        return mapOf(
            "kind" to "dashboard",
            "apiBase" to "/api",
            "me" to meVm(),
            "appointments" to appts
        )
    }

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

    // ── Private helpers: typed where existing REST DTOs cover the full shape ──

    private fun appointmentVm(row: AppointmentListRow, slots: Map<UUID, TimeSlotEntity>): AppointmentRestDto {
        val a = row.appointment
        val start = resolveStart(a, slots)
        val end = resolveEnd(a, slots)
        return AppointmentRestDto(
            id = a.id.toString(),
            patientId = a.patientId.toString(),
            patientName = row.patientName,
            employeeName = row.employeeName,
            slotLabel = row.slotLabel,
            doctorId = a.employeeId.toString(),
            employeeId = a.employeeId.toString(),
            branchId = a.branchId.toString(),
            roomId = a.roomId.toString(),
            status = mapStatusToFrontend(a.status),
            statusApi = a.status.name,
            source = a.source.name,
            start = start?.toString(),
            end = end?.toString(),
            notes = a.notes,
            timeSlotId = a.timeSlotId?.toString()
        )
    }

    private fun slotVm(row: TimeSlotListRow): TimeSlotRestDto {
        val s = row.slot
        val zone = ZoneId.systemDefault()
        val start = s.slotDate.atTime(s.startTime).atZone(zone).toInstant()
        val end = s.slotDate.atTime(s.endTime).atZone(zone).toInstant()
        return TimeSlotRestDto(
            id = s.id.toString(),
            employeeId = s.employeeId.toString(),
            branchId = s.branchId.toString(),
            roomId = s.roomId.toString(),
            slotDate = s.slotDate.toString(),
            start = start.toString(),
            end = end.toString(),
            startTime = s.startTime.toString(),
            endTime = s.endTime.toString(),
            employeeName = row.employeeName,
            roomName = row.roomName,
            branchName = row.branchName,
            isAvailable = s.isAvailable
        )
    }

    private fun paymentEntityVm(p: PaymentEntity): PaymentRestDto {
        val total = p.amount.toDouble()
        val paid = p.paidAmount?.toDouble() ?: when (p.paymentStatus) {
            PaymentStatusType.PAID -> total
            PaymentStatusType.PARTIAL -> total * 0.5
            else -> 0.0
        }
        return PaymentRestDto(
            id = p.id.toString(),
            appointmentId = p.appointmentId.toString(),
            amount = p.amount.toPlainString(),
            paidAmount = p.paidAmount?.toPlainString(),
            total = total,
            paid = paid,
            paymentMethod = p.paymentMethod.name,
            paymentStatus = p.paymentStatus.name,
            method = p.paymentMethod.name,
            notes = p.notes ?: "",
            createdBy = p.createdBy.toString()
        )
    }

    private fun buildReportsStats(
        rows: List<AppointmentListRow>,
        slots: Map<UUID, TimeSlotEntity>,
        payments: List<PaymentEntity>
    ): ReportsStatsDto {
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
        val scheduleLoad = if (slotCount > 0) min(100, (total * 100.0 / slotCount).roundToInt()) else 0

        return ReportsStatsDto(
            totalPatients = patientMvcService.listAll().size,
            totalAppointments = total,
            totalDoctors = employeeRepository.findAllOrdered().count { it.isActive },
            onlineAppointments = online,
            frontDeskAppointments = frontDesk,
            avgAppointmentTime = avgMin,
            cancelRate = (cancelRate * 10).roundToInt() / 10.0,
            noShowRate = (noShowRate * 10).roundToInt() / 10.0,
            satisfactionRate = null,
            scheduleLoad = scheduleLoad,
            bookedCount = booked,
            confirmedCount = confirmed,
            canceledCount = canceled,
            noShowCount = noShow,
            totalRevenue = totalRevenue.toDouble(),
            paidRevenue = paidRevenue.toDouble()
        )
    }

    // ── Private helpers: remain as Map (shape differs from existing REST DTOs) ──

    private fun auditVm(e: AuditLogEntity, employees: Map<UUID, com.bialger.domain.core.entity.EmployeeEntity>): AuditLogEntryDto {
        val emp = employees[e.employeeId]
        val ts = e.timestamp.toString()
        return AuditLogEntryDto(
            id = e.id.toString(),
            employeeId = e.employeeId.toString(),
            userId = e.employeeId.toString(),
            employeeName = emp?.fullName ?: e.employeeId.toString(),
            action = e.action,
            entityType = e.entityType,
            entityId = e.entityId?.toString(),
            oldValue = e.oldValue,
            newValue = e.newValue,
            ipAddress = e.ipAddress,
            userAgent = e.userAgent,
            timestamp = ts,
            ts = ts,
            diff = AuditDiffDto(old = e.oldValue, new = e.newValue)
        )
    }

    private fun currentPermissions(): Map<String, Any> = currentUserContextService.currentOrThrow().permissions

    private fun meVm(): MeRestDto = currentUserContextService.currentOrThrow().toMeDto()

    private fun branchesVm(): List<BranchRestDto> {
        val orgNames = organizationRepository.findAllOrdered().associate { it.id to it.name }
        return branchRepository.findAllOrdered().map { b ->
            BranchRestDto(
                id = b.id.toString(),
                organizationId = b.organizationId.toString(),
                organizationName = orgNames[b.organizationId],
                name = b.name,
                address = b.address,
                phone = b.phone,
                isActive = b.isActive,
                startTime = b.startTime.toString(),
                endTime = b.endTime.toString()
            )
        }
    }

    private fun usersVm(): List<EmployeeRestDto> {
        val allBranchIds = branchRepository.findAllOrdered().map { it.id.toString() }
        return employeeRepository.findAllOrdered().map { e ->
            val scoped = employeeBranchRepository.findByEmployeeId(e.id).map { it.branchId.toString() }
            val branchScope = if (scoped.isNotEmpty()) scoped else allBranchIds
            val roleLink = employeeRoleRepository.findByEmployeeId(e.id).firstOrNull()
            val roleEnt = roleLink?.roleId?.let { roleRepository.findById(it).orElse(null) }
            val roleIdStr = roleLink?.roleId?.toString() ?: ""
            EmployeeRestDto(
                id = e.id.toString(),
                fullName = e.fullName,
                email = e.email,
                phone = e.phone,
                isActive = e.isActive,
                login = e.email ?: e.id.toString().take(8),
                name = e.fullName,
                role = roleIdStr,
                roleId = roleIdStr,
                roleCode = roleEnt?.name ?: "STAFF",
                roleLabel = roleEnt?.displayName ?: roleEnt?.name ?: "Сотрудник",
                specialtyIds = emptyList(),
                branchIds = branchScope,
                branchScope = branchScope,
                workStartTime = e.workStartTime?.toString(),
                workEndTime = e.workEndTime?.toString()
            )
        }
    }

    private fun roomsVm(): List<RoomRestDto> =
        roomRepository.findAllOrdered().map { r ->
            RoomRestDto(
                id = r.id.toString(),
                branchId = r.branchId.toString(),
                name = r.name,
                description = r.description,
                isActive = r.isActive
            )
        }

    private fun patientVm(
        p: com.bialger.domain.patient.entity.PatientEntity,
        iconStrings: List<String> = emptyList()
    ): PatientRestDto {
        val bd = p.birthDate?.toString()
        return PatientRestDto(
            id = p.id.toString(),
            organizationId = p.organizationId.toString(),
            cardNumber = p.cardNumber,
            fullName = p.fullName,
            gender = p.gender?.name,
            birthDate = bd,
            dob = bd,
            phone = p.phone,
            email = p.email,
            registrationAddress = p.registrationAddress,
            residenceAddress = p.residenceAddress,
            localityType = p.localityType?.name,
            citizenship = p.citizenship,
            identityDocument = p.identityDocument,
            omsPolicy = p.omsPolicy,
            snils = p.snils,
            insuranceOrganization = p.insuranceOrganization,
            contactPerson = p.contactPerson,
            guardian = p.guardian,
            profession = p.profession,
            workplace = p.workplace,
            icons = iconStrings
        )
    }

    private fun settingsSectionsVm(permissions: Map<String, Any>): List<Map<String, String>> {
        fun allow(key: String): Boolean = permissions[key] as? Boolean == true
        val sections = mutableListOf<Map<String, String>>()

        if (allow("canReadOrganizations")) {
            sections += mapOf("id" to "organizations", "label" to "Организации", "apiPath" to "/api/organizations", "tab" to "org")
        }
        if (allow("canReadBranches")) {
            sections += mapOf("id" to "branches", "label" to "Филиалы", "apiPath" to "/api/branches", "tab" to "org")
        }
        if (allow("canReadEmployees")) {
            sections += mapOf("id" to "employees", "label" to "Пользователи", "apiPath" to "/api/employees", "tab" to "org")
        }
        if (allow("canReadPermissions")) {
            sections += mapOf("id" to "access-permissions", "label" to "Права доступа", "apiPath" to "/api/access", "tab" to "org")
        }

        if (allow("canViewSchedule")) {
            sections += mapOf("id" to "rooms", "label" to "Кабинеты", "apiPath" to "/api/rooms", "tab" to "clinical")
            sections += mapOf("id" to "time-slots", "label" to "Слоты расписания", "apiPath" to "/api/time-slots", "tab" to "clinical")
        }
        if (allow("canViewPatients")) {
            sections += mapOf("id" to "patients", "label" to "Пациенты", "apiPath" to "/api/patients", "tab" to "clinical")
        }
        if (allow("canViewAppointments")) {
            sections += mapOf("id" to "appointments", "label" to "Записи", "apiPath" to "/api/appointments", "tab" to "clinical")
        }
        if (allow("canViewSettings")) {
            sections += mapOf("id" to "services", "label" to "Услуги", "apiPath" to "/api/catalog/services", "tab" to "clinical")
            sections += mapOf("id" to "templates", "label" to "Шаблоны", "apiPath" to "/api/catalog/templates", "tab" to "clinical")
            sections += mapOf("id" to "patient-tag-types", "label" to "Значки пациентов", "apiPath" to "/api/catalog/patient-tag-types", "tab" to "clinical")
            sections += mapOf("id" to "integrations", "label" to "Интеграции", "apiPath" to "/api/catalog/integrations", "tab" to "org")
        }

        if (allow("canViewInventory")) {
            sections += mapOf("id" to "inventory", "label" to "Склад (ТМЦ)", "apiPath" to "/api/inventory-items", "tab" to "finance")
        }
        if (allow("canViewAudit")) {
            sections += mapOf("id" to "audit", "label" to "Журнал аудита", "apiPath" to "/api/audit-logs", "tab" to "finance")
        }
        if (allow("canViewReports")) {
            sections += mapOf("id" to "reports-summary", "label" to "Сводка отчётов", "apiPath" to "/api/reports/summary", "tab" to "finance")
        }
        return sections
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
}
