package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.util.UUID

data class RoomListRow(
    val room: RoomEntity,
    val branchName: String
)

@Singleton
class RoomMvcService(
    private val roomRepository: RoomRepository,
    private val branchRepository: BranchRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listRows(): List<RoomListRow> {
        val rooms = roomRepository.findAllOrdered()
        val branchNames = branchRepository.findAllOrdered().associate { it.id to it.name }
        return rooms.map { r ->
            RoomListRow(r, branchNames[r.branchId] ?: r.branchId.toString())
        }
    }

    fun getById(id: UUID): RoomEntity? = roomRepository.findById(id).orElse(null)

    fun create(branchId: UUID, name: String, description: String?, isActive: Boolean): RoomEntity {
        require(branchRepository.findById(branchId).isPresent) { "Филиал не найден" }
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = RoomEntity(
            id = id,
            branchId = branchId,
            name = n,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            isActive = isActive
        )
        roomRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(id: UUID, branchId: UUID, name: String, description: String?, isActive: Boolean): RoomEntity {
        require(branchRepository.findById(branchId).isPresent) { "Филиал не найден" }
        val existing = roomRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val updated = existing.copy(
            branchId = branchId,
            name = n,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            isActive = isActive
        )
        roomRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = roomRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        roomRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "rooms"
        const val EVENT_NAME = "rooms-change"
    }
}
