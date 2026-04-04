package com.bialger.graphql

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.finance.entity.PaymentEntity
import com.bialger.domain.finance.repository.PaymentRepository
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.mvc.PatientMvcService
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import com.bialger.graphql.model.AppointmentGql
import com.bialger.graphql.model.AppointmentPageGql
import com.bialger.graphql.model.BranchGql
import com.bialger.graphql.model.EmployeeGql
import com.bialger.graphql.model.PageInfoGql
import com.bialger.graphql.model.PatientGql
import com.bialger.graphql.model.PatientPageGql
import com.bialger.graphql.model.PatientUpsertInput
import com.bialger.graphql.model.PaymentGql
import com.bialger.graphql.model.RoomGql
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class GraphqlFacadeService(
    private val patientMvcService: PatientMvcService,
    private val appointmentMvcService: AppointmentMvcService,
    private val paymentRepository: PaymentRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository
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
        val all = appointmentMvcService.listRows()
            .asSequence()
            .map { it.appointment }
            .filter { patientId == null || it.patientId == patientId }
            .filter { status == null || it.status == status }
            .map(::toAppointmentGql)
            .toList()
        return toAppointmentPage(all, p, s)
    }

    fun listAllAppointments(): List<AppointmentGql> =
        appointmentMvcService.listRows().map { toAppointmentGql(it.appointment) }

    fun listAllPayments(): List<PaymentGql> = paymentRepository.findAllOrdered().map(::toPaymentGql)

    fun listAllEmployees(): List<EmployeeGql> = employeeRepository.findAllOrdered().map(::toEmployeeGql)

    fun listAllBranches(): List<BranchGql> = branchRepository.findAllOrdered().map(::toBranchGql)

    fun listAllRooms(): List<RoomGql> = roomRepository.findAllOrdered().map(::toRoomGql)

    fun getAppointment(id: UUID): AppointmentGql? = appointmentMvcService.getById(id)?.let(::toAppointmentGql)

    fun listPatientAppointments(patientId: UUID, page: Int, size: Int): AppointmentPageGql {
        val p = sanitizePage(page)
        val s = sanitizeSize(size)
        val all = appointmentMvcService.listRows()
            .asSequence()
            .map { it.appointment }
            .filter { it.patientId == patientId }
            .map(::toAppointmentGql)
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
        return toAppointmentGql(updated)
    }

    fun markAppointmentArrived(appointmentId: UUID): AppointmentGql {
        val updated = appointmentMvcService.updateStatus(appointmentId, AppointmentStatus.ARRIVED)
        return toAppointmentGql(updated)
    }

    fun markAppointmentNoShow(appointmentId: UUID): AppointmentGql {
        val updated = appointmentMvcService.updateStatus(appointmentId, AppointmentStatus.NO_SHOW)
        return toAppointmentGql(updated)
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
        return toAppointmentGql(updated)
    }

    fun appointmentPatient(parent: AppointmentGql): PatientGql? =
        parseUuid(parent.patientId, "patientId").let { getPatient(it) }

    fun appointmentEmployee(parent: AppointmentGql): EmployeeGql? =
        employeeRepository.findById(parseUuid(parent.employeeId, "employeeId"))
            .orElse(null)
            ?.let(::toEmployeeGql)

    fun appointmentBranch(parent: AppointmentGql): BranchGql? =
        branchRepository.findById(parseUuid(parent.branchId, "branchId"))
            .orElse(null)
            ?.let(::toBranchGql)

    fun appointmentRoom(parent: AppointmentGql): RoomGql? =
        roomRepository.findById(parseUuid(parent.roomId, "roomId"))
            .orElse(null)
            ?.let(::toRoomGql)

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

    private fun toAppointmentGql(entity: AppointmentEntity): AppointmentGql = AppointmentGql(
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
        updatedAt = entity.updatedAt?.toString()
    )

    private fun toPaymentGql(entity: PaymentEntity): PaymentGql = PaymentGql(
        id = entity.id.toString(),
        appointmentId = entity.appointmentId.toString(),
        amount = entity.amount.toPlainString(),
        paymentMethod = entity.paymentMethod,
        paymentStatus = entity.paymentStatus,
        notes = entity.notes,
        createdBy = entity.createdBy.toString(),
        createdAt = entity.createdAt?.toString()
    )

    private fun toEmployeeGql(entity: EmployeeEntity): EmployeeGql = EmployeeGql(
        id = entity.id.toString(),
        fullName = entity.fullName,
        email = entity.email,
        phone = entity.phone,
        isActive = entity.isActive
    )

    private fun toBranchGql(entity: BranchEntity): BranchGql = BranchGql(
        id = entity.id.toString(),
        organizationId = entity.organizationId.toString(),
        name = entity.name,
        address = entity.address,
        phone = entity.phone,
        isActive = entity.isActive
    )

    private fun toRoomGql(entity: RoomEntity): RoomGql = RoomGql(
        id = entity.id.toString(),
        branchId = entity.branchId.toString(),
        name = entity.name,
        description = entity.description,
        isActive = entity.isActive
    )

    private fun parseUuid(raw: String, field: String): UUID =
        runCatching { UUID.fromString(raw) }
            .getOrElse { throw IllegalArgumentException("$field must be a valid UUID") }

    private fun sanitizePage(page: Int): Int = page.coerceAtLeast(0)

    private fun sanitizeSize(size: Int): Int = size.coerceIn(1, MAX_PAGE_SIZE)

    companion object {
        private const val MAX_PAGE_SIZE = 100
    }
}
