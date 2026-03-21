package com.bialger.domain.clinical.mvc

import com.bialger.domain.clinical.entity.TemplateEntity
import com.bialger.domain.clinical.enums.TemplateType
import com.bialger.domain.clinical.repository.TemplateRepository
import com.bialger.domain.mvc.formCheckboxOn
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

@Singleton
class ClinicalTemplateMvcService(
    private val templateRepository: TemplateRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<TemplateEntity> = templateRepository.findAllOrdered()

    fun getById(id: UUID): TemplateEntity? = templateRepository.findById(id).orElse(null)

    fun create(
        name: String,
        type: TemplateType,
        specialtyId: UUID?,
        employeeId: UUID?,
        content: String,
        isActive: Boolean
    ): TemplateEntity {
        val n = name.trim()
        val c = content.trim()
        require(n.isNotEmpty() && c.isNotEmpty())
        val now = Instant.now()
        val id = UUID.randomUUID()
        val entity = TemplateEntity(
            id = id,
            name = n,
            type = type,
            specialtyId = specialtyId,
            employeeId = employeeId,
            content = c,
            isActive = isActive,
            createdAt = now,
            updatedAt = now
        )
        templateRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(
        id: UUID,
        name: String,
        type: TemplateType,
        specialtyId: UUID?,
        employeeId: UUID?,
        content: String,
        isActive: Boolean
    ): TemplateEntity {
        val existing = templateRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        val c = content.trim()
        require(n.isNotEmpty() && c.isNotEmpty())
        val now = Instant.now()
        val updated = existing.copy(
            name = n,
            type = type,
            specialtyId = specialtyId,
            employeeId = employeeId,
            content = c,
            isActive = isActive,
            updatedAt = now
        )
        templateRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = templateRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        templateRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "templates"
        const val EVENT_NAME = "templates-change"

        fun parseType(raw: String): TemplateType = TemplateType.valueOf(raw.trim())

        fun parseActive(raw: String): Boolean = raw.formCheckboxOn()
    }
}
