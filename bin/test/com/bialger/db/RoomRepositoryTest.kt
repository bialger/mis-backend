package com.bialger.db

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoomRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.UUID

@MicronautTest(transactional = true)
class RoomRepositoryTest(
    private val roomRepository: RoomRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    "save and findById" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Орг", codeOkpo = "1", address = "A"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Филиал", isActive = true))

        val room = RoomEntity(id = UUID.randomUUID(), branchId = branchId, name = "Кабинет 101", isActive = true)
        roomRepository.save(room)

        val found = roomRepository.findById(room.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Кабинет 101"
        found.branchId shouldBe branchId
    }

    "findByBranchId" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Орг2", codeOkpo = "2", address = "B"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Филиал2", isActive = true))

        roomRepository.save(RoomEntity(id = UUID.randomUUID(), branchId = branchId, name = "101", isActive = true))
        roomRepository.save(RoomEntity(id = UUID.randomUUID(), branchId = branchId, name = "102", isActive = false))

        val rooms = roomRepository.findByBranchId(branchId)
        rooms shouldHaveSize 2
    }

    "findByBranchIdAndIsActive" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Орг3", codeOkpo = "3", address = "C"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Филиал3", isActive = true))

        roomRepository.save(RoomEntity(id = UUID.randomUUID(), branchId = branchId, name = "Активный", isActive = true))
        roomRepository.save(RoomEntity(id = UUID.randomUUID(), branchId = branchId, name = "Неактивный", isActive = false))

        val activeRooms = roomRepository.findByBranchIdAndIsActive(branchId, true)
        activeRooms shouldHaveSize 1
        activeRooms.first().name shouldBe "Активный"
    }
})
