package com.bialger.db

import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.clinical.entity.MedicalRecordEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.clinical.entity.PrescriptionEntity
import com.bialger.domain.clinical.enums.PrescriptionType
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.clinical.repository.MedicalRecordRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.clinical.repository.PrescriptionRepository
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
class PrescriptionRepositoryTest(
    private val prescriptionRepository: PrescriptionRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val patientRepository: PatientRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository
) : StringSpec({

    fun createMedicalRecord(): UUID {
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
        return recordId
    }

    "save and findById" {
        val recordId = createMedicalRecord()
        val entity = PrescriptionEntity(
            id = UUID.randomUUID(),
            medicalRecordId = recordId,
            type = PrescriptionType.MEDICATION,
            description = "Paracetamol 500mg"
        )
        prescriptionRepository.save(entity)

        val found = prescriptionRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.type shouldBe PrescriptionType.MEDICATION
        found.description shouldBe "Paracetamol 500mg"
    }

    "findByMedicalRecordId" {
        val recordId = createMedicalRecord()
        prescriptionRepository.save(PrescriptionEntity(UUID.randomUUID(), recordId, PrescriptionType.PROCEDURE, "X-ray"))
        prescriptionRepository.save(PrescriptionEntity(UUID.randomUUID(), recordId, PrescriptionType.LAB_TEST, "Blood count"))

        val list = prescriptionRepository.findByMedicalRecordId(recordId)
        list shouldHaveSize 2
    }
})
