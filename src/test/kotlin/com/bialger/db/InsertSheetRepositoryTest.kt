package com.bialger.db

import com.bialger.db.entity.AppointmentEntity
import com.bialger.db.entity.BranchEntity
import com.bialger.db.entity.EmployeeEntity
import com.bialger.db.enums.AppointmentSource
import com.bialger.db.enums.AppointmentStatus
import com.bialger.db.entity.InsertSheetEntity
import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.entity.PatientEntity
import com.bialger.db.entity.RoomEntity
import com.bialger.db.entity.TimeSlotEntity
import com.bialger.db.repository.AppointmentRepository
import com.bialger.db.repository.BranchRepository
import com.bialger.db.repository.EmployeeRepository
import com.bialger.db.repository.InsertSheetRepository
import com.bialger.db.repository.OrganizationRepository
import com.bialger.db.repository.PatientRepository
import com.bialger.db.repository.RoomRepository
import com.bialger.db.repository.TimeSlotRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@MicronautTest(transactional = true)
class InsertSheetRepositoryTest(
    private val insertSheetRepository: InsertSheetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository,
    private val patientRepository: PatientRepository,
    private val roomRepository: RoomRepository,
    private val timeSlotRepository: TimeSlotRepository
) : StringSpec({

    fun createAppointment(): Triple<UUID, UUID, UUID> {
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
        return Triple(appointmentId, empId, patientId)
    }

    "save and findById" {
        val (appointmentId, empId, patientId) = createAppointment()

        val entity = InsertSheetEntity(
            id = UUID.randomUUID(),
            patientId = patientId,
            appointmentId = appointmentId,
            employeeId = empId,
            formType = "REFERRAL",
            content = "Referral content"
        )
        insertSheetRepository.save(entity)

        val found = insertSheetRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.formType shouldBe "REFERRAL"
        found.content shouldBe "Referral content"
    }

    "findByPatientId" {
        val (appointmentId, empId, patientId) = createAppointment()

        insertSheetRepository.save(InsertSheetEntity(UUID.randomUUID(), patientId, appointmentId, empId, "REFERRAL"))
        insertSheetRepository.save(InsertSheetEntity(UUID.randomUUID(), patientId, appointmentId, empId, "CONSENT"))

        val list = insertSheetRepository.findByPatientId(patientId)
        list shouldHaveSize 2
    }

    "findByAppointmentId" {
        val (appointmentId, empId, patientId) = createAppointment()

        insertSheetRepository.save(InsertSheetEntity(UUID.randomUUID(), patientId, appointmentId, empId, "FORM"))

        val list = insertSheetRepository.findByAppointmentId(appointmentId)
        list shouldHaveSize 1
        list.first().appointmentId shouldBe appointmentId
    }
})
