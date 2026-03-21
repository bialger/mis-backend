package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.SpecialtyEntity
import com.bialger.domain.core.repository.SpecialtyRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class SpecialtyMvcService(
    private val specialtyRepository: SpecialtyRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<SpecialtyEntity> = specialtyRepository.findAllOrdered()

    fun getById(id: UUID): SpecialtyEntity? = specialtyRepository.findById(id).orElse(null)

    fun create(name: String, description: String?): SpecialtyEntity {
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = SpecialtyEntity(id = id, name = n, description = description?.trim()?.takeIf { it.isNotEmpty() })
        specialtyRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(id: UUID, name: String, description: String?): SpecialtyEntity {
        val existing = specialtyRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val updated = existing.copy(name = n, description = description?.trim()?.takeIf { it.isNotEmpty() })
        specialtyRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = specialtyRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        specialtyRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "specialties"
        const val EVENT_NAME = "specialties-change"
    }
}
