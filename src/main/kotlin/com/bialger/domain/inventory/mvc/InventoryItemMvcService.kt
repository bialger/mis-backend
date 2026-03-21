package com.bialger.domain.inventory.mvc

import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.inventory.entity.InventoryItemEntity
import com.bialger.domain.inventory.repository.InventoryCategoryRepository
import com.bialger.domain.inventory.repository.InventoryItemRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.util.UUID

data class InventoryItemListRow(
    val item: InventoryItemEntity,
    val categoryName: String,
    val branchName: String,
    val roomName: String?
)

@Singleton
class InventoryItemMvcService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryCategoryRepository: InventoryCategoryRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listRows(): List<InventoryItemListRow> {
        val items = inventoryItemRepository.findAllOrdered()
        val cats = inventoryCategoryRepository.findAllOrdered().associate { it.id to it.name }
        val branches = branchRepository.findAllOrdered().associate { it.id to it.name }
        val rooms = roomRepository.findAllOrdered().associate { it.id to it.name }
        return items.map { i ->
            InventoryItemListRow(
                i,
                cats[i.categoryId] ?: i.categoryId.toString(),
                branches[i.branchId] ?: i.branchId.toString(),
                i.roomId?.let { rooms[it] }
            )
        }
    }

    fun getById(id: UUID): InventoryItemEntity? = inventoryItemRepository.findById(id).orElse(null)

    fun create(
        categoryId: UUID,
        branchId: UUID,
        roomId: UUID?,
        name: String,
        unit: String?,
        quantity: BigDecimal,
        minQuantity: BigDecimal?,
        costPrice: BigDecimal?
    ): InventoryItemEntity {
        require(inventoryCategoryRepository.findById(categoryId).isPresent) { "Категория не найдена" }
        require(branchRepository.findById(branchId).isPresent) { "Филиал не найден" }
        if (roomId != null) {
            require(roomRepository.findById(roomId).isPresent) { "Кабинет не найден" }
        }
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = InventoryItemEntity(
            id = id,
            categoryId = categoryId,
            branchId = branchId,
            roomId = roomId,
            name = n,
            unit = unit?.trim()?.takeIf { it.isNotEmpty() },
            quantity = quantity,
            minQuantity = minQuantity,
            costPrice = costPrice
        )
        inventoryItemRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(
        id: UUID,
        categoryId: UUID,
        branchId: UUID,
        roomId: UUID?,
        name: String,
        unit: String?,
        quantity: BigDecimal,
        minQuantity: BigDecimal?,
        costPrice: BigDecimal?
    ): InventoryItemEntity {
        require(inventoryCategoryRepository.findById(categoryId).isPresent) { "Категория не найдена" }
        require(branchRepository.findById(branchId).isPresent) { "Филиал не найден" }
        if (roomId != null) {
            require(roomRepository.findById(roomId).isPresent) { "Кабинет не найден" }
        }
        val existing = inventoryItemRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val updated = existing.copy(
            categoryId = categoryId,
            branchId = branchId,
            roomId = roomId,
            name = n,
            unit = unit?.trim()?.takeIf { it.isNotEmpty() },
            quantity = quantity,
            minQuantity = minQuantity,
            costPrice = costPrice
        )
        inventoryItemRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = inventoryItemRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        inventoryItemRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "inventory-items"
        const val EVENT_NAME = "inventory-items-change"

        fun parseDecimal(raw: String, defaultIfEmpty: BigDecimal = BigDecimal.ZERO): BigDecimal {
            val t = raw.trim()
            if (t.isEmpty()) return defaultIfEmpty
            return BigDecimal(t)
        }

        fun parseDecimalOpt(raw: String): BigDecimal? =
            raw.trim().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) }
    }
}
