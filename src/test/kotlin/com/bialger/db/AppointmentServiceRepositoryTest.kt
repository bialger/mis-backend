package com.bialger.db

import com.bialger.db.enums.AppointmentSource
import com.bialger.db.enums.AppointmentStatus
import com.bialger.db.entity.AppointmentEntity
import com.bialger.db.entity.AppointmentServiceEntity
import com.bialger.db.entity.BranchEntity
import com.bialger.db.entity.EmployeeEntity
import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.entity.PatientEntity
import com.bialger.db.entity.RoomEntity
import com.bialger.db.entity.ServiceEntity
import com.bialger.db.entity.TimeSlotEntity
import com.bialger.db.repository.AppointmentRepository
import com.bialger.db.repository.AppointmentServiceRepository
import com.bialger.db.repository.BranchRepository
import com.bialger.db.repository.EmployeeRepository
import com.bialger.db.repository.OrganizationRepository
import com.bialger.db.repository.PatientRepository
import com.bialger.db.repository.RoomRepository
import com.bialger.db.repository.ServiceRepository
import com.bialger.db.repository.TimeSlotRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@MicronautTest(transactional = true)
class AppointmentServiceRepositoryTest(
    private val appointmentServiceRepository: AppointmentServiceRepository,
    private val appointmentRepository: AppointmentRepository,
    private val serviceRepository: ServiceRepository,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository
) : StringSpec({

    fun createAppointmentAndService(): Pair<UUID, UUID> {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))
        val roomId = UUID.randomUUID()
        roomRepository.save(RoomEntity(id = roomId, branchId = branchId, name = "Room", isActive = true))
        val patientId = UUID.randomUUID()
        patientRepository.save(PatientEntity(id = patientId, cardNumber = "C1", organizationId = orgId, fullName = "P"))
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "D", email = "d@test.mis", passwordHash = "x", isActive = true))
        val slotId = UUID.randomUUID()
        timeSlotRepository.save(TimeSlotEntity(slotId, empId, roomId, branchId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0)))
        val appointmentId = UUID.randomUUID()
        appointmentRepository.save(AppointmentEntity(appointmentId, patientId, empId, slotId, branchId, roomId, AppointmentStatus.SCHEDULED, AppointmentSource.MANUAL))
        val serviceId = UUID.randomUUID()
        serviceRepository.save(ServiceEntity(id = serviceId, name = "Consultation", price = BigDecimal("100"), branchId = branchId))
        return appointmentId to serviceId
    }

    "save and findById" {
        val (appointmentId, serviceId) = createAppointmentAndService()
        val entity = AppointmentServiceEntity(
            id = UUID.randomUUID(),
            appointmentId = appointmentId,
            serviceId = serviceId,
            quantity = 2,
            price = BigDecimal("200")
        )
        appointmentServiceRepository.save(entity)

        val found = appointmentServiceRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.quantity shouldBe 2
        found.price shouldBe BigDecimal("200.00")
    }

    "findByAppointmentId" {
        val (appointmentId, serviceId) = createAppointmentAndService()
        appointmentServiceRepository.save(AppointmentServiceEntity(UUID.randomUUID(), appointmentId, serviceId, 1, BigDecimal("100")))
        appointmentServiceRepository.save(AppointmentServiceEntity(UUID.randomUUID(), appointmentId, serviceId, 2, BigDecimal("200")))

        val list = appointmentServiceRepository.findByAppointmentId(appointmentId)
        list shouldHaveSize 2
    }

    "findByServiceId" {
        val (appointmentId, serviceId) = createAppointmentAndService()
        appointmentServiceRepository.save(AppointmentServiceEntity(UUID.randomUUID(), appointmentId, serviceId, 1, BigDecimal("100")))

        val list = appointmentServiceRepository.findByServiceId(serviceId)
        list shouldHaveSize 1
    }
})
