package com.bialger.api

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeBranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import com.bialger.domain.system.repository.AuditLogRepository
import com.bialger.auth.domain.PasswordHasher
import com.bialger.support.TestAuthHeaders
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.transaction.SynchronousTransactionManager
import io.micronaut.data.model.Pageable
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@MicronautTest(transactional = false)
class PublicBookingApiTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val organizationRepository: OrganizationRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val roleRepository: RoleRepository,
    private val employeeRepository: EmployeeRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository,
    private val auditLogRepository: AuditLogRepository,
    private val passwordHasher: PasswordHasher,
    private val transactionManager: SynchronousTransactionManager<Connection>
) : StringSpec({
    val createdOrganizationIds = linkedSetOf<UUID>()
    val createdBranchIds = linkedSetOf<UUID>()
    val createdRoomIds = linkedSetOf<UUID>()
    val createdEmployeeIds = linkedSetOf<UUID>()
    val createdSlotIds = linkedSetOf<UUID>()
    val createdAppointmentIds = linkedSetOf<UUID>()
    val createdPatientIds = linkedSetOf<UUID>()

    data class BookingFixture(
        val organizationId: UUID,
        val branchId: UUID,
        val roomId: UUID,
        val doctorId: UUID,
        val slotId: UUID?,
        val slotDate: LocalDate
    )

    fun safeWrite(block: () -> Unit) {
        runCatching { transactionManager.executeWrite { block() } }
    }

    afterTest {
        createdAppointmentIds.forEach { id -> safeWrite { appointmentRepository.deleteById(id) } }
        createdAppointmentIds.clear()
        createdPatientIds.forEach { id -> safeWrite { patientRepository.deleteById(id) } }
        createdPatientIds.clear()
        createdSlotIds.forEach { id -> safeWrite { timeSlotRepository.deleteById(id) } }
        createdSlotIds.clear()
        createdEmployeeIds.forEach { id ->
            safeWrite {
                auditLogRepository.findByEmployeeId(id, Pageable.from(0, 1000))
                    .content
                    .forEach { log -> auditLogRepository.deleteById(log.id) }
            }
            safeWrite { employeeBranchRepository.deleteByEmployeeId(id) }
            safeWrite { employeeRoleRepository.deleteByEmployeeId(id) }
            safeWrite { employeeRepository.deleteById(id) }
        }
        createdEmployeeIds.clear()
        createdRoomIds.forEach { id -> safeWrite { roomRepository.deleteById(id) } }
        createdRoomIds.clear()
        createdBranchIds.forEach { id -> safeWrite { branchRepository.deleteById(id) } }
        createdBranchIds.clear()
        createdOrganizationIds.forEach { id -> safeWrite { organizationRepository.deleteById(id) } }
        createdOrganizationIds.clear()
    }

    fun createFixture(createSlot: Boolean = true): BookingFixture = transactionManager.executeWrite {
        val org = OrganizationEntity(
            id = UUID.randomUUID(),
            name = "Public Booking Org ${UUID.randomUUID().toString().take(8)}",
            createdAt = Instant.now()
        )
        organizationRepository.save(org)
        createdOrganizationIds += org.id

        val branch = BranchEntity(
            id = UUID.randomUUID(),
            organizationId = org.id,
            name = "Public Booking Branch ${UUID.randomUUID().toString().take(8)}",
            isActive = true,
            createdAt = Instant.now(),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(20, 0)
        )
        branchRepository.save(branch)
        createdBranchIds += branch.id

        val room = RoomEntity(
            id = UUID.randomUUID(),
            branchId = branch.id,
            name = "PB-101",
            isActive = true
        )
        roomRepository.save(room)
        createdRoomIds += room.id

        val doctor = EmployeeEntity(
            id = UUID.randomUUID(),
            fullName = "Public Doctor ${UUID.randomUUID().toString().take(6)}",
            email = "public-doctor-${UUID.randomUUID()}@mis.local",
            passwordHash = passwordHasher.hash("DoctorPass123!"),
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            workStartTime = LocalTime.of(9, 0),
            workEndTime = LocalTime.of(18, 0)
        )
        employeeRepository.save(doctor)
        createdEmployeeIds += doctor.id
        val doctorRole = roleRepository.findByName("DOCTOR")
            ?: roleRepository.findByName("HEAD")
            ?: roleRepository.findAllOrdered().firstOrNull()
            ?: throw IllegalStateException("No role found for public booking fixture")
        employeeRoleRepository.save(doctor.id, doctorRole.id)
        employeeBranchRepository.save(doctor.id, branch.id)

        val slotDate = LocalDate.now().plusDays(1)
        val slotId = if (createSlot) {
            val slot = TimeSlotEntity(
                id = UUID.randomUUID(),
                employeeId = doctor.id,
                roomId = room.id,
                branchId = branch.id,
                slotDate = slotDate,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(10, 30),
                isAvailable = true
            )
            timeSlotRepository.save(slot)
            createdSlotIds += slot.id
            slot.id
        } else {
            null
        }

        BookingFixture(
            organizationId = org.id,
            branchId = branch.id,
            roomId = room.id,
            doctorId = doctor.id,
            slotId = slotId,
            slotDate = slotDate
        )
    }

    fun <T> noAuth(request: MutableHttpRequest<T>): MutableHttpRequest<T> =
        request.header(TestAuthHeaders.NO_AUTH, "1")

    "anonymous GET public booking endpoints return data" {
        val fixture = createFixture()

        val branchesResp = client.toBlocking().exchange(
            noAuth(HttpRequest.GET<Any>("/api/public/booking/branches")),
            String::class.java
        )
        branchesResp.status shouldBe HttpStatus.OK
        objectMapper.readTree(branchesResp.body()).isArray shouldBe true

        val doctorsResp = client.toBlocking().exchange(
            noAuth(HttpRequest.GET<Any>("/api/public/booking/doctors?branchId=${fixture.branchId}")),
            String::class.java
        )
        doctorsResp.status shouldBe HttpStatus.OK
        val doctors = objectMapper.readTree(doctorsResp.body())
        doctors.isArray shouldBe true
        doctors.any { it.path("id").asText() == fixture.doctorId.toString() } shouldBe true

        val slotsResp = client.toBlocking().exchange(
            noAuth(
                HttpRequest.GET<Any>(
                    "/api/public/booking/slots?branchId=${fixture.branchId}&employeeId=${fixture.doctorId}&slotDate=${fixture.slotDate}"
                )
            ),
            String::class.java
        )
        slotsResp.status shouldBe HttpStatus.OK
        val slots = objectMapper.readTree(slotsResp.body())
        slots.isArray shouldBe true
        slots.any { it.path("timeSlotId").asText() == fixture.slotId.toString() } shouldBe true
    }

    "public slots are generated from branch and doctor work hours when DB has no predefined slots" {
        val fixture = createFixture(createSlot = false)
        val slotsResp = client.toBlocking().exchange(
            noAuth(
                HttpRequest.GET<Any>(
                    "/api/public/booking/slots?branchId=${fixture.branchId}&employeeId=${fixture.doctorId}&slotDate=${fixture.slotDate}"
                )
            ),
            String::class.java
        )
        slotsResp.status shouldBe HttpStatus.OK
        val slots = objectMapper.readTree(slotsResp.body())
        slots.isArray shouldBe true
        (slots.size() > 0) shouldBe true
        slots.any { it.path("startTime").asText() == "09:00" && it.path("endTime").asText() == "09:30" } shouldBe true
        slots.any { it.path("free").asBoolean() } shouldBe true
    }

    "anonymous POST /api/public/booking/appointments creates patient and appointment" {
        val fixture = createFixture()
        val slotId = fixture.slotId ?: error("Fixture slot must exist")
        val body = """
            {
              "branchId":"${fixture.branchId}",
              "employeeId":"${fixture.doctorId}",
              "timeSlotId":"$slotId",
              "fullName":"Public Booking Patient",
              "phone":"+79990001122",
              "comment":"from public form"
            }
        """.trimIndent()
        val response = client.toBlocking().exchange(
            noAuth(
                HttpRequest.POST("/api/public/booking/appointments", body)
                    .contentType(MediaType.APPLICATION_JSON)
            ),
            String::class.java
        )

        response.status shouldBe HttpStatus.OK
        val tree = objectMapper.readTree(response.body())
        tree.path("status").asText() shouldBe "SCHEDULED"
        tree.path("source").asText() shouldBe "ONLINE"

        val patientId = UUID.fromString(tree.path("patientId").asText())
        val appointmentId = UUID.fromString(tree.path("appointmentId").asText())
        createdPatientIds += patientId
        createdAppointmentIds += appointmentId

        transactionManager.executeWrite {
            val appointment = appointmentRepository.findById(appointmentId).orElseThrow()
            appointment.status shouldBe AppointmentStatus.SCHEDULED
            appointment.source shouldBe AppointmentSource.ONLINE
            patientRepository.findById(patientId).isPresent shouldBe true
        }
    }

    "anonymous booking can create appointment for generated slot without timeSlotId" {
        val fixture = createFixture(createSlot = false)
        val slotsResp = client.toBlocking().exchange(
            noAuth(
                HttpRequest.GET<Any>(
                    "/api/public/booking/slots?branchId=${fixture.branchId}&employeeId=${fixture.doctorId}&slotDate=${fixture.slotDate}"
                )
            ),
            String::class.java
        )
        val slots = objectMapper.readTree(slotsResp.body())
        val firstFree = slots.firstOrNull { it.path("free").asBoolean() }
            ?: error("Expected at least one free generated slot")
        val body = """
            {
              "branchId":"${fixture.branchId}",
              "employeeId":"${fixture.doctorId}",
              "slotDate":"${firstFree.path("slotDate").asText()}",
              "startTime":"${firstFree.path("startTime").asText()}",
              "endTime":"${firstFree.path("endTime").asText()}",
              "fullName":"Generated Slot Patient",
              "phone":"+79990001123"
            }
        """.trimIndent()
        val response = client.toBlocking().exchange(
            noAuth(
                HttpRequest.POST("/api/public/booking/appointments", body)
                    .contentType(MediaType.APPLICATION_JSON)
            ),
            String::class.java
        )
        response.status shouldBe HttpStatus.OK
        val tree = objectMapper.readTree(response.body())
        tree.path("status").asText() shouldBe "SCHEDULED"
        tree.path("source").asText() shouldBe "ONLINE"
        val patientId = UUID.fromString(tree.path("patientId").asText())
        val appointmentId = UUID.fromString(tree.path("appointmentId").asText())
        createdPatientIds += patientId
        createdAppointmentIds += appointmentId
        transactionManager.executeWrite {
            val appointment = appointmentRepository.findById(appointmentId).orElseThrow()
            appointment.timeSlotId?.let { createdSlotIds += it }
        }
    }

    "invalid UUID and date query params return 400" {
        val badBranch = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                noAuth(HttpRequest.GET<Any>("/api/public/booking/doctors?branchId=not-uuid")),
                String::class.java
            )
        }
        badBranch.status shouldBe HttpStatus.BAD_REQUEST

        val fixture = createFixture()
        val badDate = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                noAuth(
                    HttpRequest.GET<Any>(
                        "/api/public/booking/slots?branchId=${fixture.branchId}&employeeId=${fixture.doctorId}&slotDate=2026-99-99"
                    )
                ),
                String::class.java
            )
        }
        badDate.status shouldBe HttpStatus.BAD_REQUEST
    }

    "branch doctor slot mismatch returns 400" {
        val fixture = createFixture()
        val otherBranch = BranchEntity(
            id = UUID.randomUUID(),
            organizationId = fixture.organizationId,
            name = "Other branch ${UUID.randomUUID().toString().take(6)}",
            isActive = true,
            createdAt = Instant.now(),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(20, 0)
        )
        transactionManager.executeWrite {
            branchRepository.save(otherBranch)
            createdBranchIds += otherBranch.id
        }

        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                noAuth(
                    HttpRequest.POST(
                        "/api/public/booking/appointments",
                        """
                        {
                          "branchId":"${otherBranch.id}",
                          "employeeId":"${fixture.doctorId}",
                          "timeSlotId":"${fixture.slotId}",
                          "fullName":"Mismatch",
                          "phone":"+79990001133"
                        }
                        """.trimIndent()
                    ).contentType(MediaType.APPLICATION_JSON)
                ),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.BAD_REQUEST
    }

    "double booking same slot returns 409" {
        val fixture = createFixture()
        val slotId = fixture.slotId ?: error("Fixture slot must exist")
        val requestBody = """
            {
              "branchId":"${fixture.branchId}",
              "employeeId":"${fixture.doctorId}",
              "timeSlotId":"$slotId",
              "fullName":"First Book",
              "phone":"+79990001144"
            }
        """.trimIndent()

        val first = client.toBlocking().exchange(
            noAuth(
                HttpRequest.POST("/api/public/booking/appointments", requestBody)
                    .contentType(MediaType.APPLICATION_JSON)
            ),
            String::class.java
        )
        first.status shouldBe HttpStatus.OK
        val firstJson = objectMapper.readTree(first.body())
        createdPatientIds += UUID.fromString(firstJson.path("patientId").asText())
        createdAppointmentIds += UUID.fromString(firstJson.path("appointmentId").asText())

        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                noAuth(
                    HttpRequest.POST("/api/public/booking/appointments", requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                ),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.CONFLICT
        ex.response.getBody(String::class.java).orElse("") shouldContain "Slot"
    }

    "anonymous internal API calls are denied with 401" {
        val exAppointments = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                noAuth(HttpRequest.GET<Any>("/api/appointments?page=0&size=1")),
                String::class.java
            )
        }
        exAppointments.status shouldBe HttpStatus.UNAUTHORIZED

        val exPatients = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                noAuth(HttpRequest.GET<Any>("/api/patients?page=0&size=1")),
                String::class.java
            )
        }
        exPatients.status shouldBe HttpStatus.UNAUTHORIZED
    }
})
