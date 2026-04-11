package com.bialger.domain.scheduling.mvc

import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import com.bialger.domain.system.AuditLogService
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

data class AppointmentListRow(
    val appointment: AppointmentEntity,
    val patientName: String,
    val employeeName: String,
    val branchName: String,
    val roomName: String,
    val slotLabel: String?
)

@Singleton
class AppointmentMvcService(
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val roomRepository: RoomRepository,
    private val branchRepository: BranchRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val auditLogService: AuditLogService,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listRows(): List<AppointmentListRow> {
        val appts = appointmentRepository.findAllOrdered()
        if (appts.isEmpty()) return emptyList()
        val patientIds = appts.map { it.patientId }.distinct()
        val employeeIds = appts.map { it.employeeId }.distinct()
        val branchIds = appts.map { it.branchId }.distinct()
        val roomIds = appts.map { it.roomId }.distinct()
        val slotIds = appts.mapNotNull { it.timeSlotId }.distinct()
        val patients =
            if (patientIds.isEmpty()) emptyMap()
            else patientRepository.findByIds(patientIds).associate { it.id to it.fullName }
        val employees =
            if (employeeIds.isEmpty()) emptyMap()
            else employeeRepository.findByIds(employeeIds).associate { it.id to it.fullName }
        val branches =
            if (branchIds.isEmpty()) emptyMap()
            else branchRepository.findByIds(branchIds).associate { it.id to it.name }
        val rooms =
            if (roomIds.isEmpty()) emptyMap()
            else roomRepository.findByIds(roomIds).associate { it.id to it.name }
        val slotMap =
            if (slotIds.isEmpty()) emptyMap()
            else timeSlotRepository.findByIds(slotIds).associate { it.id to it }
        return appts.map { a ->
            val slotLabel = a.timeSlotId?.let { sid ->
                slotMap[sid]?.let { "${it.slotDate} ${it.startTime}–${it.endTime}" }
            }
            AppointmentListRow(
                a,
                patients[a.patientId] ?: a.patientId.toString(),
                employees[a.employeeId] ?: a.employeeId.toString(),
                branches[a.branchId] ?: a.branchId.toString(),
                rooms[a.roomId] ?: a.roomId.toString(),
                slotLabel
            )
        }
    }

    fun getById(id: UUID): AppointmentEntity? = appointmentRepository.findById(id).orElse(null)

    fun create(
        patientId: UUID,
        employeeId: UUID,
        timeSlotId: UUID?,
        branchId: UUID,
        roomId: UUID,
        status: AppointmentStatus,
        source: AppointmentSource,
        notes: String?,
        createdBy: UUID?
    ): AppointmentEntity {
        require(patientRepository.findById(patientId).isPresent) { "Пациент не найден" }
        require(employeeRepository.findById(employeeId).isPresent) { "Сотрудник не найден" }
        if (timeSlotId != null) {
            require(timeSlotRepository.findById(timeSlotId).isPresent) { "Слот не найден" }
        }
        require(branchRepository.findById(branchId).isPresent) { "Филиал не найден" }
        require(roomRepository.findById(roomId).isPresent) { "Кабинет не найден" }
        if (createdBy != null) {
            require(employeeRepository.findById(createdBy).isPresent) { "Кто создал — сотрудник не найден" }
        }
        val id = UUID.randomUUID()
        val now = Instant.now()
        val entity = AppointmentEntity(
            id = id,
            patientId = patientId,
            employeeId = employeeId,
            timeSlotId = timeSlotId,
            branchId = branchId,
            roomId = roomId,
            status = status,
            source = source,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            createdBy = createdBy,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.save(entity)
        writeAuditLog(
            actorId = createdBy ?: employeeId,
            action = "CREATED",
            entityId = id,
            newValue = """{"status":"${status.name}","patientId":"$patientId"}"""
        )
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), patientId.toString())
        return entity
    }

    fun update(
        id: UUID,
        patientId: UUID,
        employeeId: UUID,
        timeSlotId: UUID?,
        branchId: UUID,
        roomId: UUID,
        status: AppointmentStatus,
        source: AppointmentSource,
        notes: String?,
        createdBy: UUID?
    ): AppointmentEntity {
        require(patientRepository.findById(patientId).isPresent) { "Пациент не найден" }
        require(employeeRepository.findById(employeeId).isPresent) { "Сотрудник не найден" }
        if (timeSlotId != null) {
            require(timeSlotRepository.findById(timeSlotId).isPresent) { "Слот не найден" }
        }
        require(branchRepository.findById(branchId).isPresent) { "Филиал не найден" }
        require(roomRepository.findById(roomId).isPresent) { "Кабинет не найден" }
        if (createdBy != null) {
            require(employeeRepository.findById(createdBy).isPresent) { "Кто создал — сотрудник не найден" }
        }
        val existing = appointmentRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val updated = existing.copy(
            patientId = patientId,
            employeeId = employeeId,
            timeSlotId = timeSlotId,
            branchId = branchId,
            roomId = roomId,
            status = status,
            source = source,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            createdBy = createdBy,
            updatedAt = Instant.now()
        )
        appointmentRepository.update(updated)
        writeAuditLog(
            actorId = createdBy ?: employeeId,
            action = "UPDATED",
            entityId = id,
            oldValue = """{"status":"${existing.status.name}"}""",
            newValue = """{"status":"${status.name}","patientId":"$patientId"}"""
        )
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), patientId.toString())
        return updated
    }

    fun delete(id: UUID) {
        val existing = appointmentRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        writeAuditLog(
            actorId = existing.employeeId,
            action = "DELETED",
            entityId = id,
            oldValue = """{"status":"${existing.status.name}","patientId":"${existing.patientId}"}"""
        )
        appointmentRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.patientId.toString())
    }

    fun updateStatus(id: UUID, status: AppointmentStatus): AppointmentEntity {
        val existing = appointmentRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val updated = existing.copy(status = status, updatedAt = Instant.now())
        appointmentRepository.update(updated)
        writeAuditLog(
            actorId = existing.employeeId,
            action = "STATUS_CHANGED",
            entityId = id,
            oldValue = """{"status":"${existing.status.name}"}""",
            newValue = """{"status":"${status.name}"}"""
        )
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), existing.patientId.toString())
        return updated
    }

    private fun writeAuditLog(
        actorId: UUID,
        action: String,
        entityId: UUID,
        oldValue: String? = null,
        newValue: String? = null
    ) {
        auditLogService.log(
            actorId = actorId,
            action = action,
            entityType = AuditLogService.APPOINTMENT,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue
        )
    }

    companion object {
        const val TOPIC = "appointments"
        const val EVENT_NAME = "appointments-change"

        fun parseStatus(raw: String): AppointmentStatus =
            runCatching { AppointmentStatus.valueOf(raw.trim()) }
                .getOrElse { throw IllegalArgumentException("Некорректный статус") }

        fun parseSource(raw: String): AppointmentSource =
            runCatching { AppointmentSource.valueOf(raw.trim()) }
                .getOrElse { throw IllegalArgumentException("Некорректный источник") }
    }
}
