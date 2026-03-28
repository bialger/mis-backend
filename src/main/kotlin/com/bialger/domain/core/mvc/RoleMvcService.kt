package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.RoleEntity
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class RoleMvcService(
    private val roleRepository: RoleRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<RoleEntity> = roleRepository.findAllOrdered()

    fun getById(id: UUID): RoleEntity? = roleRepository.findById(id).orElse(null)

    fun create(name: String, displayName: String?, description: String?): RoleEntity {
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = RoleEntity(
            id = id,
            name = n,
            displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() }
        )
        roleRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(id: UUID, name: String, displayName: String?, description: String?): RoleEntity {
        val existing = roleRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val updated = existing.copy(
            name = n,
            displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() }
        )
        roleRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = roleRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        roleRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "roles"
        const val EVENT_NAME = "roles-change"
    }
}
