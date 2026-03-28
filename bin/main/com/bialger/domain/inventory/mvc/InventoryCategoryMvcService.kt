package com.bialger.domain.inventory.mvc

import com.bialger.domain.inventory.entity.InventoryCategoryEntity
import com.bialger.domain.inventory.repository.InventoryCategoryRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class InventoryCategoryMvcService(
    private val inventoryCategoryRepository: InventoryCategoryRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<InventoryCategoryEntity> = inventoryCategoryRepository.findAllOrdered()

    fun getById(id: UUID): InventoryCategoryEntity? = inventoryCategoryRepository.findById(id).orElse(null)

    fun create(name: String, description: String?): InventoryCategoryEntity {
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = InventoryCategoryEntity(
            id = id,
            name = n,
            description = description?.trim()?.takeIf { it.isNotEmpty() }
        )
        inventoryCategoryRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(id: UUID, name: String, description: String?): InventoryCategoryEntity {
        val existing = inventoryCategoryRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val updated = existing.copy(
            name = n,
            description = description?.trim()?.takeIf { it.isNotEmpty() }
        )
        inventoryCategoryRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = inventoryCategoryRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        inventoryCategoryRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "inventory-categories"
        const val EVENT_NAME = "inventory-categories-change"
    }
}
