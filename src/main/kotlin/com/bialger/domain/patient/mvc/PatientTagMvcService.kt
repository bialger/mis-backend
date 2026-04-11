package com.bialger.domain.patient.mvc

import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.patient.entity.PatientTagEntity
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.patient.repository.PatientTagRepository
import com.bialger.domain.patient.repository.PatientTagTypeRepository
import com.bialger.web.DomainMvcEventEmitter
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

@Singleton
open class PatientTagMvcService(
    private val patientRepository: PatientRepository,
    private val patientTagRepository: PatientTagRepository,
    private val patientTagTypeRepository: PatientTagTypeRepository,
    private val employeeRepository: EmployeeRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    @Transactional(readOnly = true)
    open fun listTagTypeIds(patientId: UUID): List<UUID> {
        require(patientRepository.findById(patientId).isPresent) { "Пациент не найден" }
        return patientTagRepository.findByPatientId(patientId).map { it.tagTypeId }.distinct()
    }

    @Transactional
    open fun replaceTags(patientId: UUID, tagTypeIds: Collection<UUID>) {
        val patient = patientRepository.findById(patientId).orElseThrow { IllegalArgumentException("Пациент не найден") }
        val distinct = tagTypeIds.toSet()
        for (tid in distinct) {
            val t = patientTagTypeRepository.findById(tid).orElseThrow {
                IllegalArgumentException("Тип пометки не найден")
            }
            require(t.isActive) { "Тип пометки неактивен: ${t.code}" }
        }
        patientTagRepository.findByPatientId(patientId).forEach { patientTagRepository.deleteById(it.id) }
        val createdBy = employeeRepository.findAllOrdered().firstOrNull()?.id
        val now = Instant.now()
        for (tid in distinct) {
            patientTagRepository.save(
                PatientTagEntity(
                    id = UUID.randomUUID(),
                    patientId = patientId,
                    tagTypeId = tid,
                    createdBy = createdBy,
                    createdAt = now
                )
            )
        }
        domainMvcEventEmitter.notify(PatientMvcService.TOPIC, "UPDATED", patientId.toString(), patient.fullName)
    }
}
