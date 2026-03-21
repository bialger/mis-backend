package com.bialger.db

import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.entity.AppointmentEntity
import com.bialger.domain.attachment.enums.FileType
import com.bialger.domain.attachment.entity.AttachmentEntity
import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.clinical.entity.MedicalRecordEntity
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.domain.attachment.repository.AttachmentRepository
import com.bialger.domain.clinical.repository.MedicalRecordRepository
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.patient.repository.PatientRepository
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
class AttachmentRepositoryTest(
    private val attachmentRepository: AttachmentRepository,
    private val patientRepository: PatientRepository,
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val appointmentRepository: AppointmentRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val medicalRecordRepository: MedicalRecordRepository
) : StringSpec({

    fun createPatientAndEmployee(): Pair<UUID, UUID> {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val patientId = UUID.randomUUID()
        patientRepository.save(PatientEntity(id = patientId, cardNumber = "C1", organizationId = orgId, fullName = "P"))
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "D", email = "d@test.mis", passwordHash = "x", isActive = true))
        return patientId to empId
    }

    fun createAppointment(patientId: UUID, empId: UUID): UUID {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "2", address = "B"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))
        val roomId = UUID.randomUUID()
        roomRepository.save(RoomEntity(id = roomId, branchId = branchId, name = "Room", isActive = true))
        val slotId = UUID.randomUUID()
        timeSlotRepository.save(TimeSlotEntity(slotId, empId, roomId, branchId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0)))
        val appointmentId = UUID.randomUUID()
        appointmentRepository.save(AppointmentEntity(appointmentId, patientId, empId, slotId, branchId, roomId, AppointmentStatus.SCHEDULED, AppointmentSource.MANUAL))
        return appointmentId
    }

    "save and findById" {
        val (patientId, empId) = createPatientAndEmployee()
        val entity = AttachmentEntity(
            id = UUID.randomUUID(),
            patientId = patientId,
            fileName = "report.pdf",
            fileType = FileType.PDF,
            filePath = "/uploads/report.pdf",
            fileSize = 1024L,
            uploadedBy = empId
        )
        attachmentRepository.save(entity)

        val found = attachmentRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.fileName shouldBe "report.pdf"
        found.fileType shouldBe FileType.PDF
    }

    "findByPatientId" {
        val (patientId, empId) = createPatientAndEmployee()
        attachmentRepository.save(AttachmentEntity(UUID.randomUUID(), patientId, fileName = "a.pdf", fileType = FileType.PDF, filePath = "/a", uploadedBy = empId))
        attachmentRepository.save(AttachmentEntity(UUID.randomUUID(), patientId, fileName = "b.pdf", fileType = FileType.PDF, filePath = "/b", uploadedBy = empId))

        val list = attachmentRepository.findByPatientId(patientId)
        list shouldHaveSize 2
    }

    "findByAppointmentId" {
        val (patientId, empId) = createPatientAndEmployee()
        val appointmentId = createAppointment(patientId, empId)
        attachmentRepository.save(AttachmentEntity(UUID.randomUUID(), patientId, appointmentId = appointmentId, fileName = "x.pdf", fileType = FileType.PDF, filePath = "/x", uploadedBy = empId))

        val list = attachmentRepository.findByAppointmentId(appointmentId)
        list shouldHaveSize 1
        list.first().appointmentId shouldBe appointmentId
    }

    "findByMedicalRecordId" {
        val (patientId, empId) = createPatientAndEmployee()
        val appointmentId = createAppointment(patientId, empId)
        val recordId = UUID.randomUUID()
        medicalRecordRepository.save(MedicalRecordEntity(recordId, appointmentId, patientId, empId))
        attachmentRepository.save(AttachmentEntity(UUID.randomUUID(), patientId, medicalRecordId = recordId, fileName = "m.pdf", fileType = FileType.PDF, filePath = "/m", uploadedBy = empId))

        val list = attachmentRepository.findByMedicalRecordId(recordId)
        list shouldHaveSize 1
    }
})
