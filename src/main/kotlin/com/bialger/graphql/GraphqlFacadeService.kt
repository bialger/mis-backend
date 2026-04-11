package com.bialger.graphql

import com.bialger.api.dto.RoomRestDto
import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeBranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.finance.entity.PaymentEntity
import com.bialger.domain.finance.repository.PaymentRepository
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.mvc.PatientMvcService
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.mvc.AppointmentListRow
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import com.bialger.graphql.model.AppointmentGql
import com.bialger.graphql.model.AppointmentPageGql
import com.bialger.graphql.model.BranchGql
import com.bialger.graphql.model.EmployeeGql
import com.bialger.graphql.model.PageInfoGql
import com.bialger.graphql.model.PatientGql
import com.bialger.graphql.model.PatientPageGql
import com.bialger.graphql.model.PatientUpsertInput
import com.bialger.graphql.model.PaymentGql
import jakarta.inject.Singleton
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Singleton
class GraphqlFacadeService(
    private val patientMvcService: PatientMvcService,
    private val appointmentMvcService: AppointmentMvcService,
    private val paymentRepository: PaymentRepository,
    private val employeeRepository: EmployeeRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val roleRepository: RoleRepository,
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository
) {

    fun listPatients(page: Int, size: Int, search: String?): PatientPageGql {
        val p = sanitizePage(page)
        val s = sanitizeSize(size)
        val all = patientMvcService.search(search).map(::toPatientGql)
        return toPatientPage(all, p, s)
    }

    fun listAllPatients(): List<PatientGql> = patientMvcService.listAll().map(::toPatientGql)

    fun getPatient(id: UUID): PatientGql? = patientMvcService.getById(id)?.let(::toPatientGql)

    fun listAppointments(page: Int, size: Int, patientId: UUID?, status: AppointmentStatus?): AppointmentPageGql {
        val p = sanitizePage(page)
        val s = sanitizeSize(size)
        val slots = loadSlotsMap()
        val all = appointmentMvcService.listRows()
            .asSequence()
            .filter { patientId == null || it.appointment.patientId == patientId }
            .filter { status == null || it.appointment.status == status }
            .map { toAppointmentGql(it, slots) }
            .toList()
        return toAppointmentPage(all, p, s)
    }

    fun listAllAppointments(): List<AppointmentGql> {
        val slots = loadSlotsMap()
        return appointmentMvcService.listRows().map { toAppointmentGql(it, slots) }
    }

    fun listAllPayments(): List<PaymentGql> = paymentRepository.findAllOrdered().map(::toPaymentGql)

    fun listAllEmployees(): List<EmployeeGql> {
        val allBranchIds = branchRepository.findAllOrdered().map { it.id.toString() }
        return employeeRepository.findAllOrdered().map { toEmployeeGql(it, allBranchIds) }
    }

    fun listAllBranches(): List<BranchGql> {
        val orgMap = organizationRepository.findAllOrdered().associate { it.id to it.name }
        return branchRepository.findAllOrdered().map { toBranchGql(it, orgMap) }
    }

    fun listAllRooms(): List<RoomRestDto> = roomRepository.findAllOrdered().map(::toRoomDto)

    fun getAppointment(id: UUID): AppointmentGql? {
        val slots = loadSlotsMap()
        return appointmentMvcService.listRows().find { it.appointment.id == id }
            ?.let { toAppointmentGql(it, slots) }
    }

    fun listPatientAppointments(patientId: UUID, page: Int, size: Int): AppointmentPageGql {
        val p = sanitizePage(page)
        val s = sanitizeSize(size)
        val slots = loadSlotsMap()
        val all = appointmentMvcService.listRows()
            .asSequence()
            .filter { it.appointment.patientId == patientId }
            .map { toAppointmentGql(it, slots) }
            .toList()
        return toAppointmentPage(all, p, s)
    }

    fun createPatient(input: PatientUpsertInput): PatientGql {
        val organizationId = parseUuid(input.organizationId, "organizationId")
        val created = patientMvcService.create(
            organizationId = organizationId,
            cardNumber = input.cardNumber,
            fullName = input.fullName,
            gender = input.gender,
            birthDate = input.birthDate?.let(PatientMvcService::parseBirthDate),
            phone = input.phone,
            email = input.email,
            registrationAddress = input.registrationAddress,
            residenceAddress = input.residenceAddress,
            localityType = input.localityType,
            citizenship = input.citizenship,
            identityDocument = input.identityDocument,
            omsPolicy = input.omsPolicy,
            snils = input.snils,
            insuranceOrganization = input.insuranceOrganization,
            contactPerson = input.contactPerson,
            guardian = input.guardian,
            profession = input.profession,
            workplace = input.workplace
        )
        return toPatientGql(created)
    }

    fun updatePatient(id: UUID, input: PatientUpsertInput): PatientGql {
        val organizationId = parseUuid(input.organizationId, "organizationId")
        val updated = patientMvcService.update(
            id = id,
            organizationId = organizationId,
            cardNumber = input.cardNumber,
            fullName = input.fullName,
            gender = input.gender,
            birthDate = input.birthDate?.let(PatientMvcService::parseBirthDate),
            phone = input.phone,
            email = input.email,
            registrationAddress = input.registrationAddress,
            residenceAddress = input.residenceAddress,
            localityType = input.localityType,
            citizenship = input.citizenship,
            identityDocument = input.identityDocument,
            omsPolicy = input.omsPolicy,
            snils = input.snils,
            insuranceOrganization = input.insuranceOrganization,
            contactPerson = input.contactPerson,
            guardian = input.guardian,
            profession = input.profession,
            workplace = input.workplace
        )
        return toPatientGql(updated)
    }

    fun confirmAppointment(appointmentId: UUID): AppointmentGql {
        val updated = appointmentMvcService.updateStatus(appointmentId, AppointmentStatus.CONFIRMED)
        return toAppointmentGqlBasic(updated)
    }

    fun markAppointmentArrived(appointmentId: UUID): AppointmentGql {
        val updated = appointmentMvcService.updateStatus(appointmentId, AppointmentStatus.ARRIVED)
        return toAppointmentGqlBasic(updated)
    }

    fun markAppointmentNoShow(appointmentId: UUID): AppointmentGql {
        val updated = appointmentMvcService.updateStatus(appointmentId, AppointmentStatus.NO_SHOW)
        return toAppointmentGqlBasic(updated)
    }

    fun cancelAppointment(appointmentId: UUID, reason: String?): AppointmentGql {
        val current = appointmentMvcService.getById(appointmentId) ?: throw IllegalArgumentException("Not found")
        val cancelNote = reason?.trim()?.takeIf { it.isNotEmpty() }
        val mergedNotes = when {
            cancelNote == null -> current.notes
            current.notes.isNullOrBlank() -> cancelNote
            else -> "${current.notes}\nCancel reason: $cancelNote"
        }
        val updated = appointmentMvcService.update(
            id = current.id,
            patientId = current.patientId,
            employeeId = current.employeeId,
            timeSlotId = current.timeSlotId,
            branchId = current.branchId,
            roomId = current.roomId,
            status = AppointmentStatus.CANCELLED,
            source = current.source,
            notes = mergedNotes,
            createdBy = current.createdBy
        )
        return toAppointmentGqlBasic(updated)
    }

    fun appointmentPatient(parent: AppointmentGql): PatientGql? =
        parseUuid(parent.patientId, "patientId").let { getPatient(it) }

    fun appointmentEmployee(parent: AppointmentGql): EmployeeGql? {
        val allBranchIds = branchRepository.findAllOrdered().map { it.id.toString() }
        return employeeRepository.findById(parseUuid(parent.employeeId, "employeeId"))
            .orElse(null)
            ?.let { toEmployeeGql(it, allBranchIds) }
    }

    fun appointmentBranch(parent: AppointmentGql): BranchGql? {
        val orgMap = organizationRepository.findAllOrdered().associate { it.id to it.name }
        return branchRepository.findById(parseUuid(parent.branchId, "branchId"))
            .orElse(null)
            ?.let { toBranchGql(it, orgMap) }
    }

    fun appointmentRoom(parent: AppointmentGql): RoomRestDto? =
        roomRepository.findById(parseUuid(parent.roomId, "roomId"))
            .orElse(null)
            ?.let(::toRoomDto)

    fun appointmentPayments(parent: AppointmentGql): List<PaymentGql> =
        paymentRepository.findByAppointmentId(parseUuid(parent.id, "appointmentId"))
            .map(::toPaymentGql)

    private fun toPatientPage(all: List<PatientGql>, page: Int, size: Int): PatientPageGql {
        val (items, info) = slice(all, page, size)
        return PatientPageGql(items = items, pageInfo = info)
    }

    private fun toAppointmentPage(all: List<AppointmentGql>, page: Int, size: Int): AppointmentPageGql {
        val (items, info) = slice(all, page, size)
        return AppointmentPageGql(items = items, pageInfo = info)
    }

    private fun <T> slice(all: List<T>, page: Int, size: Int): Pair<List<T>, PageInfoGql> {
        val totalItems = all.size
        val fromIndex = (page * size).coerceAtMost(totalItems)
        val toIndex = (fromIndex + size).coerceAtMost(totalItems)
        val items = if (fromIndex >= toIndex) emptyList() else all.subList(fromIndex, toIndex)
        val totalPages = if (totalItems == 0) 0 else ((totalItems - 1) / size) + 1
        val info = PageInfoGql(
            page = page,
            size = size,
            totalItems = totalItems,
            totalPages = totalPages,
            hasNext = page + 1 < totalPages,
            hasPrevious = page > 0
        )
        return items to info
    }

    private fun toPatientGql(entity: PatientEntity): PatientGql = PatientGql(
        id = entity.id.toString(),
        organizationId = entity.organizationId.toString(),
        cardNumber = entity.cardNumber,
        fullName = entity.fullName,
        gender = entity.gender,
        birthDate = entity.birthDate?.toString(),
        phone = entity.phone,
        email = entity.email,
        registrationAddress = entity.registrationAddress,
        residenceAddress = entity.residenceAddress,
        localityType = entity.localityType,
        citizenship = entity.citizenship,
        identityDocument = entity.identityDocument,
        omsPolicy = entity.omsPolicy,
        snils = entity.snils,
        insuranceOrganization = entity.insuranceOrganization,
        contactPerson = entity.contactPerson,
        guardian = entity.guardian,
        profession = entity.profession,
        workplace = entity.workplace,
        createdAt = entity.createdAt?.toString(),
        updatedAt = entity.updatedAt?.toString()
    )

    private fun toAppointmentGql(row: AppointmentListRow, slots: Map<UUID, TimeSlotEntity>): AppointmentGql {
        val a = row.appointment
        return AppointmentGql(
            id = a.id.toString(),
            patientId = a.patientId.toString(),
            employeeId = a.employeeId.toString(),
            timeSlotId = a.timeSlotId?.toString(),
            branchId = a.branchId.toString(),
            roomId = a.roomId.toString(),
            status = a.status,
            source = a.source,
            notes = a.notes,
            createdBy = a.createdBy?.toString(),
            createdAt = a.createdAt?.toString(),
            updatedAt = a.updatedAt?.toString(),
            patientName = row.patientName,
            employeeName = row.employeeName,
            slotLabel = row.slotLabel,
            start = resolveStart(a, slots)?.toString(),
            end = resolveEnd(a, slots)?.toString()
        )
    }

    /** Used by mutation responses where re-fetching the full row is unnecessary. */
    private fun toAppointmentGqlBasic(entity: AppointmentEntity): AppointmentGql = AppointmentGql(
        id = entity.id.toString(),
        patientId = entity.patientId.toString(),
        employeeId = entity.employeeId.toString(),
        timeSlotId = entity.timeSlotId?.toString(),
        branchId = entity.branchId.toString(),
        roomId = entity.roomId.toString(),
        status = entity.status,
        source = entity.source,
        notes = entity.notes,
        createdBy = entity.createdBy?.toString(),
        createdAt = entity.createdAt?.toString(),
        updatedAt = entity.updatedAt?.toString(),
        patientName = null,
        employeeName = null,
        slotLabel = null,
        start = entity.createdAt?.toString(),
        end = null
    )

    private fun toPaymentGql(entity: PaymentEntity): PaymentGql = PaymentGql(
        id = entity.id.toString(),
        appointmentId = entity.appointmentId.toString(),
        amount = entity.amount.toPlainString(),
        paidAmount = entity.paidAmount?.toPlainString(),
        paymentMethod = entity.paymentMethod,
        paymentStatus = entity.paymentStatus,
        notes = entity.notes,
        createdBy = entity.createdBy.toString(),
        createdAt = entity.createdAt?.toString()
    )

    private fun toEmployeeGql(entity: EmployeeEntity, allBranchIds: List<String>): EmployeeGql {
        val roleLink = employeeRoleRepository.findByEmployeeId(entity.id).firstOrNull()
        val roleEnt = roleLink?.roleId?.let { roleRepository.findById(it).orElse(null) }
        val scoped = employeeBranchRepository.findByEmployeeId(entity.id).map { it.branchId.toString() }
        val branchScope = if (scoped.isNotEmpty()) scoped else allBranchIds
        return EmployeeGql(
            id = entity.id.toString(),
            fullName = entity.fullName,
            email = entity.email,
            phone = entity.phone,
            isActive = entity.isActive,
            login = entity.email ?: entity.id.toString().take(8),
            roleCode = roleEnt?.name ?: "STAFF",
            roleLabel = roleEnt?.displayName ?: roleEnt?.name ?: "Сотрудник",
            branchScope = branchScope
        )
    }

    private fun toBranchGql(entity: BranchEntity, orgMap: Map<UUID, String>): BranchGql = BranchGql(
        id = entity.id.toString(),
        organizationId = entity.organizationId.toString(),
        organizationName = orgMap[entity.organizationId],
        name = entity.name,
        address = entity.address,
        phone = entity.phone,
        isActive = entity.isActive,
        startTime = entity.startTime.toString(),
        endTime = entity.endTime.toString()
    )

    private fun toRoomDto(entity: RoomEntity): RoomRestDto = RoomRestDto(
        id = entity.id.toString(),
        branchId = entity.branchId.toString(),
        name = entity.name,
        description = entity.description,
        isActive = entity.isActive
    )

    private fun loadSlotsMap(): Map<UUID, TimeSlotEntity> =
        timeSlotRepository.findAllOrdered().associateBy { it.id }

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

    private fun parseUuid(raw: String, field: String): UUID =
        runCatching { UUID.fromString(raw) }
            .getOrElse { throw IllegalArgumentException("$field must be a valid UUID") }

    private fun sanitizePage(page: Int): Int = page.coerceAtLeast(0)

    private fun sanitizeSize(size: Int): Int = size.coerceIn(1, MAX_PAGE_SIZE)

    companion object {
        private const val MAX_PAGE_SIZE = 100
    }
}
