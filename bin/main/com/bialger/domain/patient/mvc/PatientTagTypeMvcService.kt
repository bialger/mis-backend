package com.bialger.domain.patient.mvc

import com.bialger.domain.mvc.formCheckboxOn
import com.bialger.domain.patient.entity.PatientTagTypeEntity
import com.bialger.domain.patient.repository.PatientTagTypeRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class PatientTagTypeMvcService(
    private val patientTagTypeRepository: PatientTagTypeRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<PatientTagTypeEntity> = patientTagTypeRepository.findAllOrdered()

    fun getById(id: UUID): PatientTagTypeEntity? = patientTagTypeRepository.findById(id).orElse(null)

    fun create(code: String, name: String, icon: String?, description: String?, isActive: Boolean): PatientTagTypeEntity {
        val c = code.trim()
        val n = name.trim()
        require(c.isNotEmpty() && n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = PatientTagTypeEntity(
            id = id,
            code = c,
            name = n,
            icon = icon?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            isActive = isActive
        )
        patientTagTypeRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(
        id: UUID,
        code: String,
        name: String,
        icon: String?,
        description: String?,
        isActive: Boolean
    ): PatientTagTypeEntity {
        val existing = patientTagTypeRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val c = code.trim()
        val n = name.trim()
        require(c.isNotEmpty() && n.isNotEmpty())
        val updated = existing.copy(
            code = c,
            name = n,
            icon = icon?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            isActive = isActive
        )
        patientTagTypeRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = patientTagTypeRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        patientTagTypeRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "patient-tag-types"
        const val EVENT_NAME = "patient-tag-types-change"
    }
}
