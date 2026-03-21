package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

data class BranchListRow(
    val branch: BranchEntity,
    val organizationName: String
)

@Singleton
class BranchMvcService(
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listRows(): List<BranchListRow> {
        val branches = branchRepository.findAllOrdered()
        if (branches.isEmpty()) return emptyList()
        val orgIds = branches.map { it.organizationId }.distinct()
        val orgNames =
            if (orgIds.isEmpty()) emptyMap()
            else organizationRepository.findByIds(orgIds).associate { it.id to it.name }
        return branches.map { b ->
            BranchListRow(b, orgNames[b.organizationId] ?: b.organizationId.toString())
        }
    }

    fun getById(id: UUID): BranchEntity? = branchRepository.findById(id).orElse(null)

    fun create(
        organizationId: UUID,
        name: String,
        address: String?,
        phone: String?,
        isActive: Boolean
    ): BranchEntity {
        require(organizationRepository.findById(organizationId).isPresent) { "Организация не найдена" }
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = BranchEntity(
            id = id,
            organizationId = organizationId,
            name = n,
            address = address?.trim()?.takeIf { it.isNotEmpty() },
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            isActive = isActive,
            createdAt = Instant.now()
        )
        branchRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(
        id: UUID,
        organizationId: UUID,
        name: String,
        address: String?,
        phone: String?,
        isActive: Boolean
    ): BranchEntity {
        require(organizationRepository.findById(organizationId).isPresent) { "Организация не найдена" }
        val existing = branchRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val updated = existing.copy(
            organizationId = organizationId,
            name = n,
            address = address?.trim()?.takeIf { it.isNotEmpty() },
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            isActive = isActive
        )
        branchRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = branchRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        branchRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "branches"
        const val EVENT_NAME = "branches-change"
    }
}
