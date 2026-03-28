package com.bialger.domain.scheduling

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@MicronautTest(transactional = true)
class TimeSlotRepositoryTest(
    private val timeSlotRepository: TimeSlotRepository,
    private val employeeRepository: EmployeeRepository,
    private val roomRepository: RoomRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    fun setupBranchRoomEmployee(): Triple<UUID, UUID, UUID> {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))
        val roomId = UUID.randomUUID()
        roomRepository.save(RoomEntity(id = roomId, branchId = branchId, name = "Room", isActive = true))
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "Doctor", email = "d@test.mis", passwordHash = "x", isActive = true))
        return Triple(branchId, roomId, empId)
    }

    "save and findById" {
        val (branchId, roomId, empId) = setupBranchRoomEmployee()
        val slot = TimeSlotEntity(
            id = UUID.randomUUID(),
            employeeId = empId,
            roomId = roomId,
            branchId = branchId,
            slotDate = LocalDate.of(2025, 3, 15),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            isAvailable = true
        )
        timeSlotRepository.save(slot)

        val found = timeSlotRepository.findById(slot.id).orElse(null)
        found.shouldNotBeNull()
        found.slotDate shouldBe LocalDate.of(2025, 3, 15)
    }

    "findByEmployeeId" {
        val (branchId, roomId, empId) = setupBranchRoomEmployee()
        timeSlotRepository.save(TimeSlotEntity(UUID.randomUUID(), empId, roomId, branchId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0)))
        timeSlotRepository.save(TimeSlotEntity(UUID.randomUUID(), empId, roomId, branchId, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0)))

        val list = timeSlotRepository.findByEmployeeId(empId)
        list shouldHaveSize 2
    }

    "findByBranchIdAndSlotDate" {
        val (branchId, roomId, empId) = setupBranchRoomEmployee()
        val date = LocalDate.of(2025, 3, 20)
        timeSlotRepository.save(TimeSlotEntity(UUID.randomUUID(), empId, roomId, branchId, date, LocalTime.of(9, 0), LocalTime.of(10, 0)))
        timeSlotRepository.save(TimeSlotEntity(UUID.randomUUID(), empId, roomId, branchId, date, LocalTime.of(11, 0), LocalTime.of(12, 0)))

        val list = timeSlotRepository.findByBranchIdAndSlotDate(branchId, date)
        list shouldHaveSize 2
    }
})
