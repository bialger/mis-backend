package com.bialger.domain.laboratory.mvc

import com.bialger.domain.laboratory.entity.LaboratoryEntity
import com.bialger.domain.laboratory.repository.LaboratoryRepository
import com.bialger.domain.mvc.formCheckboxOn
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class LaboratoryMvcService(
    private val laboratoryRepository: LaboratoryRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<LaboratoryEntity> = laboratoryRepository.findAllOrdered()

    fun getById(id: UUID): LaboratoryEntity? = laboratoryRepository.findById(id).orElse(null)

    fun create(name: String, integrationType: String?, config: String?, isActive: Boolean): LaboratoryEntity {
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val cfg = config?.trim()?.takeIf { it.isNotEmpty() }
        val entity = LaboratoryEntity(
            id = id,
            name = n,
            integrationType = integrationType?.trim()?.takeIf { it.isNotEmpty() },
            config = cfg,
            isActive = isActive
        )
        laboratoryRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(id: UUID, name: String, integrationType: String?, config: String?, isActive: Boolean): LaboratoryEntity {
        val existing = laboratoryRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val cfg = config?.trim()?.takeIf { it.isNotEmpty() }
        val updated = existing.copy(
            name = n,
            integrationType = integrationType?.trim()?.takeIf { it.isNotEmpty() },
            config = cfg,
            isActive = isActive
        )
        laboratoryRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = laboratoryRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        laboratoryRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "laboratories"
        const val EVENT_NAME = "laboratories-change"

        fun parseActive(raw: String): Boolean = raw.formCheckboxOn()
    }
}
