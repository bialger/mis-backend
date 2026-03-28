package com.bialger.domain.clinical.mvc

import com.bialger.domain.clinical.entity.MedicalRecordEntity
import com.bialger.domain.clinical.repository.MedicalRecordRepository
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

@Singleton
open class MedicalRecordMvcService(
    private val medicalRecordRepository: MedicalRecordRepository,
    private val appointmentRepository: AppointmentRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listMapsByPatientId(patientId: UUID): List<Map<String, Any?>> =
        medicalRecordRepository.findByPatientId(patientId).map { toMap(it) }

    fun getById(id: UUID): MedicalRecordEntity? = medicalRecordRepository.findById(id).orElse(null)

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
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), existing.patientId.toString())
        return updated
    }

    /**
     * Creates an empty medical record for an appointment if one does not exist yet.
     */
    fun ensureForAppointment(appointmentId: UUID): MedicalRecordEntity {
        medicalRecordRepository.findByAppointmentId(appointmentId)?.let { return it }
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
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), a.patientId.toString())
        return entity
    }

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
