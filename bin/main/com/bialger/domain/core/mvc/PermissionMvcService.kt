package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.PermissionEntity
import com.bialger.domain.core.repository.PermissionRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class PermissionMvcService(
    private val permissionRepository: PermissionRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<PermissionEntity> = permissionRepository.findAllOrdered()

    fun getById(id: UUID): PermissionEntity? = permissionRepository.findById(id).orElse(null)

    fun create(code: String, name: String?, description: String?): PermissionEntity {
        val c = code.trim()
        require(c.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = PermissionEntity(
            id = id,
            code = c,
            name = name?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() }
        )
        permissionRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.code)
        return entity
    }

    fun update(id: UUID, code: String, name: String?, description: String?): PermissionEntity {
        val existing = permissionRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val c = code.trim()
        require(c.isNotEmpty())
        val updated = existing.copy(
            code = c,
            name = name?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() }
        )
        permissionRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.code)
        return updated
    }

    fun delete(id: UUID) {
        val existing = permissionRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        permissionRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.code)
    }

    companion object {
        const val TOPIC = "permissions"
        const val EVENT_NAME = "permissions-change"
    }
}
