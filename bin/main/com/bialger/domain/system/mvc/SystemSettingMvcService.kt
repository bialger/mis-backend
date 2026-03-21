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

    /**
     * Values for [com.bialger.web.CrmShellPageData]: same keys as OpenAPI `Permissions` and `mock_api.json` → `me.permissions`.
     */
    fun resolvePermissions(): Map<String, Any> {
        val byKey = systemSettingRepository.findAllOrdered().associateBy { it.key }
        fun bool(key: String, default: Boolean): Boolean {
            val raw = byKey[key]?.value?.trim()?.lowercase() ?: return default
            return when (raw) {
                "true", "1", "yes", "да" -> true
                "false", "0", "no", "нет" -> false
                else -> raw.toBooleanStrictOrNull() ?: default
            }
        }
        fun int(key: String, default: Int): Int =
            byKey[key]?.value?.trim()?.toIntOrNull() ?: default
        return mapOf(
            "canViewFinance" to bool("canViewFinance", true),
            "canEditFinance" to bool("canEditFinance", true),
            "canViewInventory" to bool("canViewInventory", true),
            "canWriteInventory" to bool("canWriteInventory", true),
            "canManualEgiszSend" to bool("canManualEgiszSend", true),
            "canEditBackdateDays" to int("canEditBackdateDays", 0)
        )
    }

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
