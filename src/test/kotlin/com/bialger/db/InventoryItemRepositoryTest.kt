package com.bialger.db

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.inventory.entity.InventoryCategoryEntity
import com.bialger.domain.inventory.entity.InventoryItemEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.inventory.repository.InventoryCategoryRepository
import com.bialger.domain.inventory.repository.InventoryItemRepository
import com.bialger.domain.core.repository.OrganizationRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.math.BigDecimal
import java.util.UUID

@MicronautTest(transactional = true)
class InventoryItemRepositoryTest(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryCategoryRepository: InventoryCategoryRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    fun setupBranchAndCategory(): Pair<UUID, UUID> {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))
        val categoryId = UUID.randomUUID()
        inventoryCategoryRepository.save(InventoryCategoryEntity(id = categoryId, name = "Medications"))
        return branchId to categoryId
    }

    "save and findById" {
        val (branchId, categoryId) = setupBranchAndCategory()
        val entity = InventoryItemEntity(
            id = UUID.randomUUID(),
            categoryId = categoryId,
            branchId = branchId,
            name = "Bandages",
            unit = "pcs",
            quantity = BigDecimal("100"),
            minQuantity = BigDecimal("10")
        )
        inventoryItemRepository.save(entity)

        val found = inventoryItemRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Bandages"
        found.quantity.compareTo(BigDecimal("100")) shouldBe 0
    }

    "findByBranchId" {
        val (branchId, categoryId) = setupBranchAndCategory()
        inventoryItemRepository.save(InventoryItemEntity(UUID.randomUUID(), categoryId, branchId, name = "Item1", unit = "pcs", quantity = BigDecimal.ZERO))
        inventoryItemRepository.save(InventoryItemEntity(UUID.randomUUID(), categoryId, branchId, name = "Item2", unit = "pcs", quantity = BigDecimal.ZERO))

        val list = inventoryItemRepository.findByBranchId(branchId)
        list shouldHaveSize 2
    }

    "findByCategoryId" {
        val (branchId, categoryId) = setupBranchAndCategory()
        inventoryItemRepository.save(InventoryItemEntity(UUID.randomUUID(), categoryId, branchId, name = "A", unit = "pcs", quantity = BigDecimal.ZERO))

        val list = inventoryItemRepository.findByCategoryId(categoryId)
        list shouldHaveSize 1
        list.first().categoryId shouldBe categoryId
    }
})
