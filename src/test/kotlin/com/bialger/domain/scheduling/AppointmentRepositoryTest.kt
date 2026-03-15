package com.bialger.domain.scheduling

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.repository.AppointmentRepository
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
class AppointmentRepositoryTest(
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository
) : StringSpec({

    fun setupAppointmentDeps(): Pair<Pair<UUID, UUID>, Triple<UUID, UUID, UUID>> {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))
        val roomId = UUID.randomUUID()
        roomRepository.save(RoomEntity(id = roomId, branchId = branchId, name = "Room", isActive = true))
        val patientId = UUID.randomUUID()
        patientRepository.save(PatientEntity(id = patientId, cardNumber = "C1", organizationId = orgId, fullName = "Patient"))
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "Doctor", email = "d@test.mis", passwordHash = "x", isActive = true))
        val slotId = UUID.randomUUID()
        timeSlotRepository.save(TimeSlotEntity(slotId, empId, roomId, branchId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0)))
        return (patientId to empId) to Triple(branchId, roomId, slotId)
    }

    "save and findById" {
        val deps = setupAppointmentDeps()
        val (patientId, empId) = deps.first
        val (branchId, roomId, slotId) = deps.second
        val entity = AppointmentEntity(
            id = UUID.randomUUID(),
            patientId = patientId,
            employeeId = empId,
            timeSlotId = slotId,
            branchId = branchId,
            roomId = roomId,
            status = AppointmentStatus.SCHEDULED,
            source = AppointmentSource.MANUAL
        )
        appointmentRepository.save(entity)

        val found = appointmentRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.status shouldBe AppointmentStatus.SCHEDULED
        found.source shouldBe AppointmentSource.MANUAL
    }

    "findByPatientId" {
        val deps = setupAppointmentDeps()
        val (patientId, empId) = deps.first
        val (branchId, roomId, slotId) = deps.second
        appointmentRepository.save(AppointmentEntity(UUID.randomUUID(), patientId, empId, slotId, branchId, roomId, AppointmentStatus.SCHEDULED, AppointmentSource.MANUAL))
        appointmentRepository.save(AppointmentEntity(UUID.randomUUID(), patientId, empId, slotId, branchId, roomId, AppointmentStatus.CONFIRMED, AppointmentSource.ONLINE))

        val list = appointmentRepository.findByPatientId(patientId)
        list shouldHaveSize 2
    }

    "findByEmployeeId" {
        val deps = setupAppointmentDeps()
        val (patientId, empId) = deps.first
        val (branchId, roomId, slotId) = deps.second
        appointmentRepository.save(AppointmentEntity(UUID.randomUUID(), patientId, empId, slotId, branchId, roomId, AppointmentStatus.SCHEDULED, AppointmentSource.MANUAL))

        val list = appointmentRepository.findByEmployeeId(empId)
        list shouldHaveSize 1
    }

    "findByBranchId" {
        val deps = setupAppointmentDeps()
        val (patientId, empId) = deps.first
        val (branchId, roomId, slotId) = deps.second
        appointmentRepository.save(AppointmentEntity(UUID.randomUUID(), patientId, empId, slotId, branchId, roomId, AppointmentStatus.SCHEDULED, AppointmentSource.MANUAL))

        val list = appointmentRepository.findByBranchId(branchId)
        list shouldHaveSize 1
    }
})
