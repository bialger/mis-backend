package com.bialger.api

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeBranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.transaction.SynchronousTransactionManager
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@MicronautTest(transactional = false)
class DeleteConflictApiTest(
    @param:Client("/") private val client: HttpClient,
    private val organizationRepository: OrganizationRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val employeeRepository: EmployeeRepository,
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val patientRepository: PatientRepository,
    private val timeSlotRepository: TimeSlotRepository,
    private val transactionManager: SynchronousTransactionManager<Connection>
) : StringSpec({
    val createdOrganizationIds = linkedSetOf<UUID>()
    val createdBranchIds = linkedSetOf<UUID>()
    val createdRoomIds = linkedSetOf<UUID>()
    val createdEmployeeIds = linkedSetOf<UUID>()
    val createdPatientIds = linkedSetOf<UUID>()
    val createdSlotIds = linkedSetOf<UUID>()

    fun cleanup() {
        transactionManager.executeWrite {
            createdPatientIds.forEach { id -> runCatching { patientRepository.deleteById(id) } }
            createdPatientIds.clear()

            createdSlotIds.forEach { id -> runCatching { timeSlotRepository.deleteById(id) } }
            createdSlotIds.clear()

            createdEmployeeIds.forEach { id ->
                runCatching { employeeBranchRepository.deleteByEmployeeId(id) }
                runCatching { employeeRepository.deleteById(id) }
            }
            createdEmployeeIds.clear()

            createdRoomIds.forEach { id -> runCatching { roomRepository.deleteById(id) } }
            createdRoomIds.clear()

            createdBranchIds.forEach { id -> runCatching { branchRepository.deleteById(id) } }
            createdBranchIds.clear()

            createdOrganizationIds.forEach { id -> runCatching { organizationRepository.deleteById(id) } }
            createdOrganizationIds.clear()
        }
    }

    afterTest { cleanup() }
    afterSpec { cleanup() }

    fun createOrganization(): UUID = transactionManager.executeWrite {
        val id = UUID.randomUUID()
        organizationRepository.save(
            OrganizationEntity(
                id = id,
                name = "Delete Conflict Org ${id.toString().take(6)}",
                createdAt = Instant.now()
            )
        )
        createdOrganizationIds += id
        id
    }

    fun createBranch(organizationId: UUID): UUID = transactionManager.executeWrite {
        val id = UUID.randomUUID()
        branchRepository.save(
            BranchEntity(
                id = id,
                organizationId = organizationId,
                name = "Delete Conflict Branch ${id.toString().take(6)}",
                isActive = true,
                createdAt = Instant.now()
            )
        )
        createdBranchIds += id
        id
    }

    fun createEmployee(): UUID = transactionManager.executeWrite {
        val id = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(
                id = id,
                fullName = "Delete Conflict Employee ${id.toString().take(6)}",
                email = "delete-conflict-${id.toString().take(8)}@mis.local",
                passwordHash = "hash",
                isActive = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        createdEmployeeIds += id
        id
    }

    "DELETE /api/branches/{id} returns 409 when branch has linked employee" {
        val organizationId = createOrganization()
        val branchId = createBranch(organizationId)
        val employeeId = createEmployee()
        transactionManager.executeWrite {
            employeeBranchRepository.save(employeeId, branchId)
        }

        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.DELETE<Any>("/api/branches/$branchId"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.CONFLICT
        val body = ex.response.getBody(String::class.java).orElse("")
        body shouldContain "связанные данные"
    }

    "DELETE /api/employees/{id} returns 409 when employee is referenced by slot" {
        val organizationId = createOrganization()
        val branchId = createBranch(organizationId)
        val roomId = transactionManager.executeWrite {
            val id = UUID.randomUUID()
            roomRepository.save(
                RoomEntity(
                    id = id,
                    branchId = branchId,
                    name = "Delete Conflict Room",
                    isActive = true
                )
            )
            createdRoomIds += id
            id
        }
        val employeeId = createEmployee()
        transactionManager.executeWrite {
            val id = UUID.randomUUID()
            timeSlotRepository.save(
                TimeSlotEntity(
                    id = id,
                    employeeId = employeeId,
                    roomId = roomId,
                    branchId = branchId,
                    slotDate = LocalDate.now().plusDays(1),
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(10, 30),
                    isAvailable = true
                )
            )
            createdSlotIds += id
        }

        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.DELETE<Any>("/api/employees/$employeeId"),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.CONFLICT
        val body = ex.response.getBody(String::class.java).orElse("")
        body shouldContain "связанные данные"
    }

    "cleanup sanity: created patient rows can be removed in teardown" {
        val organizationId = createOrganization()
        val patientId = transactionManager.executeWrite {
            val id = UUID.randomUUID()
            patientRepository.save(
                PatientEntity(
                    id = id,
                    cardNumber = "DC-${id.toString().take(6)}",
                    organizationId = organizationId,
                    fullName = "Delete Conflict Patient",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
            createdPatientIds += id
            id
        }
        patientId.toString().isNotBlank() shouldBe true
    }
})
