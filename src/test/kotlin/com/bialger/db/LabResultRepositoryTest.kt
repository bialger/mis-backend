package com.bialger.db

import com.bialger.db.entity.AppointmentEntity
import com.bialger.db.entity.LabResultEntity
import com.bialger.db.entity.BranchEntity
import com.bialger.db.entity.EmployeeEntity
import com.bialger.db.entity.LabOrderEntity
import com.bialger.db.entity.LabOrderItemEntity
import com.bialger.db.entity.LabTestEntity
import com.bialger.db.entity.MedicalRecordEntity
import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.entity.PatientEntity
import com.bialger.db.entity.RoomEntity
import com.bialger.db.entity.TimeSlotEntity
import com.bialger.db.enums.AppointmentSource
import com.bialger.db.enums.LabResultSource
import com.bialger.db.enums.AppointmentStatus
import com.bialger.db.enums.LabOrderStatus
import com.bialger.db.repository.AppointmentRepository
import com.bialger.db.repository.BranchRepository
import com.bialger.db.repository.EmployeeRepository
import com.bialger.db.repository.LabOrderItemRepository
import com.bialger.db.repository.LabOrderRepository
import com.bialger.db.repository.LabResultRepository
import com.bialger.db.repository.LabTestRepository
import com.bialger.db.repository.MedicalRecordRepository
import com.bialger.db.repository.OrganizationRepository
import com.bialger.db.repository.PatientRepository
import com.bialger.db.repository.RoomRepository
import com.bialger.db.repository.TimeSlotRepository
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@MicronautTest(transactional = true)
class LabResultRepositoryTest(
    private val labResultRepository: LabResultRepository,
    private val labOrderItemRepository: LabOrderItemRepository,
    private val labOrderRepository: LabOrderRepository,
    private val labTestRepository: LabTestRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val patientRepository: PatientRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository
) : StringSpec({

    fun createLabOrderItem(): UUID {
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
        val recordId = UUID.randomUUID()
        medicalRecordRepository.save(MedicalRecordEntity(recordId, appointmentId, patientId, empId))
        val orderId = UUID.randomUUID()
        labOrderRepository.save(LabOrderEntity(orderId, recordId, patientId, empId, null, LabOrderStatus.CREATED))
        val testId = UUID.randomUUID()
        labTestRepository.save(LabTestEntity(testId, "Blood count", isActive = true))
        val itemId = UUID.randomUUID()
        labOrderItemRepository.save(LabOrderItemEntity(itemId, orderId, testId, BigDecimal("50")))
        return itemId
    }

    "save and findById" {
        val itemId = createLabOrderItem()
        val entity = LabResultEntity(
            id = UUID.randomUUID(),
            labOrderItemId = itemId,
            resultData = "WBC 5.2",
            source = LabResultSource.MANUAL
        )
        labResultRepository.save(entity)

        val found = labResultRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.resultData shouldBe "WBC 5.2"
        found.source shouldBe LabResultSource.MANUAL
    }

    "findByLabOrderItemId" {
        val itemId = createLabOrderItem()
        labResultRepository.save(LabResultEntity(UUID.randomUUID(), itemId, resultData = "Hb 14.5", source = LabResultSource.MANUAL))

        val found = labResultRepository.findByLabOrderItemId(itemId)
        found.shouldNotBeNull()
        found.resultData shouldBe "Hb 14.5"
    }
})
