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
     *
     * Role-specific defaults are applied first, then the DB can further restrict (but not elevate) permissions
     * for roles that are locked down (SYSADMIN, DOCTOR, NURSE).
     * For HEAD and ADMIN the DB settings take full effect.
     */
    fun resolvePermissions(roleCode: String = "ADMIN"): Map<String, Any> {
        val byKey = systemSettingRepository.findAllOrdered().associateBy { it.key }

        fun rawBool(key: String): Boolean? {
            val raw = byKey[key]?.value?.trim()?.lowercase() ?: return null
            return when (raw) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> raw.toBooleanStrictOrNull()
            }
        }

        fun rawInt(key: String): Int? = byKey[key]?.value?.trim()?.toIntOrNull()

        val roleDefaults: Map<String, Any> = when (roleCode) {
            "SYSADMIN" -> mapOf(
                "canViewFinance" to false,
                "canEditFinance" to false,
                "canViewInventory" to false,
                "canWriteInventory" to false,
                "canManualEgiszSend" to false,
                "canEditBackdateDays" to 0
            )
            "DOCTOR" -> mapOf(
                "canViewFinance" to false,
                "canEditFinance" to false,
                "canViewInventory" to false,
                "canWriteInventory" to false,
                "canManualEgiszSend" to false,
                "canEditBackdateDays" to 60
            )
            "NURSE" -> mapOf(
                "canViewFinance" to false,
                "canEditFinance" to false,
                "canViewInventory" to true,
                "canWriteInventory" to false,
                "canManualEgiszSend" to false,
                "canEditBackdateDays" to 0
            )
            "HEAD" -> mapOf(
                "canViewFinance" to true,
                "canEditFinance" to true,
                "canViewInventory" to true,
                "canWriteInventory" to true,
                "canManualEgiszSend" to true,
                "canEditBackdateDays" to 3650
            )
            else -> mapOf(
                "canViewFinance" to true,
                "canEditFinance" to true,
                "canViewInventory" to true,
                "canWriteInventory" to true,
                "canManualEgiszSend" to false,
                "canEditBackdateDays" to 0
            )
        }

        val lockedRoles = setOf("SYSADMIN", "DOCTOR", "NURSE", "ADMIN", "HEAD")
        fun bool(key: String): Boolean {
            val roleDefault = roleDefaults[key] as? Boolean ?: true
            if (roleCode in lockedRoles) return roleDefault
            return rawBool(key) ?: roleDefault
        }

        fun int(key: String): Int {
            val roleDefault = roleDefaults[key] as? Int ?: 0
            if (roleCode in lockedRoles) return roleDefault
            return rawInt(key) ?: roleDefault
        }

        return mapOf(
            "canViewFinance" to bool("canViewFinance"),
            "canEditFinance" to bool("canEditFinance"),
            "canViewInventory" to bool("canViewInventory"),
            "canWriteInventory" to bool("canWriteInventory"),
            "canManualEgiszSend" to bool("canManualEgiszSend"),
            "canEditBackdateDays" to int("canEditBackdateDays")
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
