package com.bialger.domain.attachment.mvc

import com.bialger.domain.attachment.entity.IntegrationEntity
import com.bialger.domain.attachment.enums.IntegrationType
import com.bialger.domain.attachment.repository.IntegrationRepository
import com.bialger.domain.mvc.formCheckboxOn
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

@Singleton
class IntegrationMvcService(
    private val integrationRepository: IntegrationRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<IntegrationEntity> = integrationRepository.findAllOrdered()

    fun getById(id: UUID): IntegrationEntity? = integrationRepository.findById(id).orElse(null)

    fun create(type: IntegrationType, name: String, config: String?, isActive: Boolean): IntegrationEntity {
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val cfg = config?.trim()?.takeIf { it.isNotEmpty() }
        val entity = IntegrationEntity(
            id = id,
            type = type,
            name = n,
            config = cfg,
            isActive = isActive,
            createdAt = Instant.now()
        )
        integrationRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(id: UUID, type: IntegrationType, name: String, config: String?, isActive: Boolean): IntegrationEntity {
        val existing = integrationRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val cfg = config?.trim()?.takeIf { it.isNotEmpty() }
        val updated = existing.copy(
            type = type,
            name = n,
            config = cfg,
            isActive = isActive
        )
        integrationRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = integrationRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        integrationRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "integrations"
        const val EVENT_NAME = "integrations-change"

        fun parseType(raw: String): IntegrationType = IntegrationType.valueOf(raw.trim())

        fun parseActive(raw: String): Boolean = raw.formCheckboxOn()
    }
}
