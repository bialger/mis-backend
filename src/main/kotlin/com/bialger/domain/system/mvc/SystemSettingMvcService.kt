package com.bialger.domain.system.mvc

import com.bialger.domain.mvc.parseUuidOrNull
import com.bialger.domain.system.entity.SystemSettingEntity
import com.bialger.domain.system.repository.SystemSettingRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class SystemSettingMvcService(
    private val systemSettingRepository: SystemSettingRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<SystemSettingEntity> = systemSettingRepository.findAllOrdered()

    fun getById(id: UUID): SystemSettingEntity? = systemSettingRepository.findById(id).orElse(null)

    fun create(branchId: UUID?, key: String, value: String?, description: String?): SystemSettingEntity {
        val k = key.trim()
        require(k.isNotEmpty())
        if (systemSettingRepository.findByBranchIdAndKey(branchId, k) != null) {
            throw IllegalArgumentException("Запись с таким ключом уже есть для этого филиала")
        }
        val id = UUID.randomUUID()
        val entity = SystemSettingEntity(
            id = id,
            branchId = branchId,
            key = k,
            value = value?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() }
        )
        systemSettingRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.key)
        return entity
    }

    fun update(id: UUID, branchId: UUID?, key: String, value: String?, description: String?): SystemSettingEntity {
        val existing = systemSettingRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val k = key.trim()
        require(k.isNotEmpty())
        val other = systemSettingRepository.findByBranchIdAndKey(branchId, k)
        if (other != null && other.id != id) {
            throw IllegalArgumentException("Запись с таким ключом уже есть для этого филиала")
        }
        val updated = existing.copy(
            branchId = branchId,
            key = k,
            value = value?.trim()?.takeIf { it.isNotEmpty() },
            description = description?.trim()?.takeIf { it.isNotEmpty() }
        )
        systemSettingRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.key)
        return updated
    }

    fun delete(id: UUID) {
        val existing = systemSettingRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        systemSettingRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.key)
    }

    companion object {
        const val TOPIC = "system-settings"
        const val EVENT_NAME = "system-settings-change"

        fun parseBranchId(raw: String): UUID? = raw.parseUuidOrNull()
    }
}
