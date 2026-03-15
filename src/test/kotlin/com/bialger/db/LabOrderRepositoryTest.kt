package com.bialger.db

import com.bialger.domain.laboratory.enums.LabOrderStatus
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.laboratory.entity.LabOrderEntity
import com.bialger.domain.clinical.entity.MedicalRecordEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.laboratory.repository.LabOrderRepository
import com.bialger.domain.clinical.repository.MedicalRecordRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.scheduling.repository.TimeSlotRepository
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
class LabOrderRepositoryTest(
    private val labOrderRepository: LabOrderRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val patientRepository: PatientRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository
) : StringSpec({

    fun createMedicalRecord(): Pair<UUID, UUID> {
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
        return recordId to patientId
    }

    "save and findById" {
        val (recordId, patientId) = createMedicalRecord()
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "E", email = "e@test.mis", passwordHash = "x", isActive = true))

        val entity = LabOrderEntity(
            id = UUID.randomUUID(),
            medicalRecordId = recordId,
            patientId = patientId,
            employeeId = empId,
            totalPrice = BigDecimal("100"),
            status = LabOrderStatus.CREATED
        )
        labOrderRepository.save(entity)

        val found = labOrderRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.status shouldBe LabOrderStatus.CREATED
        found.totalPrice?.compareTo(BigDecimal("100")) shouldBe 0
    }

    "findByMedicalRecordId" {
        val (recordId, patientId) = createMedicalRecord()
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "E", email = "e@test.mis", passwordHash = "x", isActive = true))

        labOrderRepository.save(LabOrderEntity(UUID.randomUUID(), recordId, patientId, empId, null, LabOrderStatus.CREATED))
        labOrderRepository.save(LabOrderEntity(UUID.randomUUID(), recordId, patientId, empId, null, LabOrderStatus.SENT))

        val list = labOrderRepository.findByMedicalRecordId(recordId)
        list shouldHaveSize 2
    }

    "findByPatientId" {
        val (recordId, patientId) = createMedicalRecord()
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "E", email = "e@test.mis", passwordHash = "x", isActive = true))
        labOrderRepository.save(LabOrderEntity(UUID.randomUUID(), recordId, patientId, empId, null, LabOrderStatus.CREATED))

        val list = labOrderRepository.findByPatientId(patientId)
        list shouldHaveSize 1
        list.first().patientId shouldBe patientId
    }

    "findByStatus" {
        val (recordId, patientId) = createMedicalRecord()
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "EX", email = "ex@test.mis", passwordHash = "x", isActive = true))
        labOrderRepository.save(LabOrderEntity(UUID.randomUUID(), recordId, patientId, empId, null, LabOrderStatus.COMPLETED))

        val list = labOrderRepository.findByStatus(LabOrderStatus.COMPLETED)
        list.any { it.medicalRecordId == recordId } shouldBe true
    }
})
