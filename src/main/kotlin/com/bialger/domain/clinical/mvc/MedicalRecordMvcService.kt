package com.bialger.domain.clinical.mvc

import com.bialger.api.dto.MedicalRecordRestDto
import com.bialger.domain.clinical.entity.MedicalRecordEntity
import com.bialger.domain.clinical.repository.MedicalRecordRepository
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.domain.system.AuditLogService
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

@Singleton
open class MedicalRecordMvcService(
    private val medicalRecordRepository: MedicalRecordRepository,
    private val appointmentRepository: AppointmentRepository,
    private val auditLogService: AuditLogService,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listByPatientId(patientId: UUID): List<MedicalRecordRestDto> {
        val records = medicalRecordRepository.findByPatientId(patientId)
        records.forEach { r ->
            auditLogService.log(
                actorId = r.employeeId,
                action = "READ_LIST",
                entityType = AuditLogService.MEDICAL_RECORD,
                entityId = r.id,
                newValue = """{"patientId":"$patientId"}"""
            )
        }
        return records.map { toDto(it) }
    }

    @Deprecated("Use listByPatientId() which returns typed DTOs", ReplaceWith("listByPatientId(patientId)"))
    fun listMapsByPatientId(patientId: UUID): List<Map<String, Any?>> =
        medicalRecordRepository.findByPatientId(patientId).map { toMap(it) }

    fun getById(id: UUID): MedicalRecordEntity? {
        val record = medicalRecordRepository.findById(id).orElse(null) ?: return null
        auditLogService.log(
            actorId = record.employeeId,
            action = "READ",
            entityType = AuditLogService.MEDICAL_RECORD,
            entityId = record.id
        )
        return record
    }

    fun update(
        id: UUID,
        complaints: String?,
        anamnesis: String?,
        examinationResults: String?,
        diseaseCourse: String?,
        procedures: String?,
        epicrisis: String?
    ): MedicalRecordEntity {
        val existing = medicalRecordRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val updated = existing.copy(
            complaints = complaints?.trim()?.takeIf { it.isNotEmpty() },
            anamnesis = anamnesis?.trim()?.takeIf { it.isNotEmpty() },
            examinationResults = examinationResults?.trim()?.takeIf { it.isNotEmpty() },
            diseaseCourse = diseaseCourse?.trim()?.takeIf { it.isNotEmpty() },
            procedures = procedures?.trim()?.takeIf { it.isNotEmpty() },
            epicrisis = epicrisis?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = Instant.now()
        )
        medicalRecordRepository.update(updated)
        auditLogService.log(
            actorId = existing.employeeId,
            action = "UPDATED",
            entityType = AuditLogService.MEDICAL_RECORD,
            entityId = id,
            oldValue = """{"isSigned":${existing.isSigned}}""",
            newValue = """{"complaints":${existing.complaints != complaints},"isSigned":${updated.isSigned}}"""
        )
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), existing.patientId.toString())
        return updated
    }

    /**
     * Creates an empty medical record for an appointment if one does not exist yet.
     */
    fun ensureForAppointment(appointmentId: UUID): MedicalRecordEntity {
        medicalRecordRepository.findByAppointmentId(appointmentId)?.let { existing ->
            auditLogService.log(
                actorId = existing.employeeId,
                action = "READ",
                entityType = AuditLogService.MEDICAL_RECORD,
                entityId = existing.id
            )
            return existing
        }
        val a = appointmentRepository.findById(appointmentId).orElseThrow { IllegalArgumentException("Appointment not found") }
        val id = UUID.randomUUID()
        val now = Instant.now()
        val entity = MedicalRecordEntity(
            id = id,
            appointmentId = appointmentId,
            patientId = a.patientId,
            employeeId = a.employeeId,
            createdAt = now,
            updatedAt = now
        )
        medicalRecordRepository.save(entity)
        auditLogService.log(
            actorId = a.employeeId,
            action = "CREATED",
            entityType = AuditLogService.MEDICAL_RECORD,
            entityId = id,
            newValue = """{"appointmentId":"$appointmentId","patientId":"${a.patientId}"}"""
        )
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), a.patientId.toString())
        return entity
    }

    fun toDto(e: MedicalRecordEntity): MedicalRecordRestDto = MedicalRecordRestDto(
        id = e.id.toString(),
        appointmentId = e.appointmentId.toString(),
        patientId = e.patientId.toString(),
        employeeId = e.employeeId.toString(),
        complaints = e.complaints,
        anamnesis = e.anamnesis,
        examinationResults = e.examinationResults,
        diseaseCourse = e.diseaseCourse,
        procedures = e.procedures,
        epicrisis = e.epicrisis,
        isSigned = e.isSigned,
        templateId = e.templateId?.toString()
    )

    fun toMap(e: MedicalRecordEntity): Map<String, Any?> = mapOf(
        "id" to e.id.toString(),
        "appointmentId" to e.appointmentId.toString(),
        "patientId" to e.patientId.toString(),
        "employeeId" to e.employeeId.toString(),
        "complaints" to e.complaints,
        "anamnesis" to e.anamnesis,
        "examinationResults" to e.examinationResults,
        "diseaseCourse" to e.diseaseCourse,
        "procedures" to e.procedures,
        "epicrisis" to e.epicrisis,
        "isSigned" to e.isSigned,
        "templateId" to e.templateId?.toString()
    )

    companion object {
        const val TOPIC = "medical-records"
        const val EVENT_NAME = "medical-records-change"
    }
}
