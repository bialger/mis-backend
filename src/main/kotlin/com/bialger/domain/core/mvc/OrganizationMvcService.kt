package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

@Singleton
class OrganizationMvcService(
    private val organizationRepository: OrganizationRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<OrganizationEntity> = organizationRepository.findAllOrdered()

    fun getById(id: UUID): OrganizationEntity? = organizationRepository.findById(id).orElse(null)

    fun create(name: String, codeOkpo: String?, codeOkud: String?, address: String?): OrganizationEntity {
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = OrganizationEntity(
            id = id,
            name = n,
            codeOkpo = codeOkpo?.trim()?.takeIf { it.isNotEmpty() },
            codeOkud = codeOkud?.trim()?.takeIf { it.isNotEmpty() },
            address = address?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = Instant.now()
        )
        organizationRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(
        id: UUID,
        name: String,
        codeOkpo: String?,
        codeOkud: String?,
        address: String?
    ): OrganizationEntity {
        val existing = organizationRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val updated = existing.copy(
            name = n,
            codeOkpo = codeOkpo?.trim()?.takeIf { it.isNotEmpty() },
            codeOkud = codeOkud?.trim()?.takeIf { it.isNotEmpty() },
            address = address?.trim()?.takeIf { it.isNotEmpty() }
        )
        organizationRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = organizationRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        organizationRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "organizations"
        const val EVENT_NAME = "organizations-change"
    }
}
