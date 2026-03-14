package com.bialger.db

import com.bialger.db.enums.AppointmentSource
import com.bialger.db.enums.AppointmentStatus
import com.bialger.db.enums.PaymentMethodType
import com.bialger.db.enums.PaymentStatusType
import com.bialger.db.entity.AppointmentEntity
import com.bialger.db.entity.BranchEntity
import com.bialger.db.entity.EmployeeEntity
import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.entity.PaymentEntity
import com.bialger.db.entity.PatientEntity
import com.bialger.db.entity.RoomEntity
import com.bialger.db.entity.TimeSlotEntity
import com.bialger.db.repository.AppointmentRepository
import com.bialger.db.repository.BranchRepository
import com.bialger.db.repository.EmployeeRepository
import com.bialger.db.repository.OrganizationRepository
import com.bialger.db.repository.PaymentRepository
import com.bialger.db.repository.PatientRepository
import com.bialger.db.repository.RoomRepository
import com.bialger.db.repository.TimeSlotRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@MicronautTest(transactional = true)
class PaymentRepositoryTest(
    private val paymentRepository: PaymentRepository,
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val patientRepository: PatientRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository
) : StringSpec({

    fun createAppointment(): Pair<UUID, UUID> {
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
        return appointmentId to empId
    }

    "save and findById" {
        val (appointmentId, empId) = createAppointment()
        val entity = PaymentEntity(
            id = UUID.randomUUID(),
            appointmentId = appointmentId,
            amount = BigDecimal("500.00"),
            paymentMethod = PaymentMethodType.CARD,
            paymentStatus = PaymentStatusType.PAID,
            createdBy = empId,
            createdAt = Instant.now()
        )
        paymentRepository.save(entity)

        val found = paymentRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.amount shouldBe BigDecimal("500.00")
        found.paymentStatus shouldBe PaymentStatusType.PAID
    }

    "findByAppointmentId" {
        val (appointmentId, empId) = createAppointment()
        paymentRepository.save(PaymentEntity(UUID.randomUUID(), appointmentId, BigDecimal("100"), PaymentMethodType.CASH, PaymentStatusType.PAID, createdBy = empId))
        paymentRepository.save(PaymentEntity(UUID.randomUUID(), appointmentId, BigDecimal("200"), PaymentMethodType.CARD, PaymentStatusType.PAID, createdBy = empId))

        val list = paymentRepository.findByAppointmentId(appointmentId)
        list shouldHaveSize 2
    }

    "findByCreatedBy" {
        val (appointmentId, empId) = createAppointment()
        paymentRepository.save(PaymentEntity(UUID.randomUUID(), appointmentId, BigDecimal("150"), PaymentMethodType.CASH, PaymentStatusType.PAID, createdBy = empId))

        val list = paymentRepository.findByCreatedBy(empId)
        list shouldHaveSize 1
        list.first().createdBy shouldBe empId
    }
})
