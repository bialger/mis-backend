package com.bialger.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.entity.RoomEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.repository.AppointmentRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Instant
import java.util.UUID

@MicronautTest
class GraphqlApiTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val organizationRepository: OrganizationRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val patientRepository: PatientRepository,
    private val appointmentRepository: AppointmentRepository
) : StringSpec({
    val createdOrganizationIds = linkedSetOf<UUID>()
    val createdEmployeeIds = linkedSetOf<UUID>()
    val createdBranchIds = linkedSetOf<UUID>()
    val createdRoomIds = linkedSetOf<UUID>()
    val createdPatientIds = linkedSetOf<UUID>()
    val createdAppointmentIds = linkedSetOf<UUID>()

    fun graphQl(query: String, variables: Map<String, Any?> = emptyMap()): JsonNode {
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "query" to query,
                "variables" to variables
            )
        )
        val req = HttpRequest.POST("/graphql", payload).contentType(MediaType.APPLICATION_JSON)
        val body = client.toBlocking().retrieve(req)
        return objectMapper.readTree(body)
    }

    fun ensureNoGraphqlErrors(response: JsonNode) {
        val errors = response.get("errors")
        if (errors != null && errors.isArray && errors.size() > 0) {
            throw AssertionError("GraphQL errors: $errors")
        }
    }

    fun createPatientViaGraphql(organizationId: String, fullName: String): String {
        val mutation = """
            mutation CreatePatient(${'$'}input: PatientUpsertInput!) {
              createPatient(input: ${'$'}input) {
                id
              }
            }
        """.trimIndent()
        val response = graphQl(
            mutation,
            mapOf(
                "input" to mapOf(
                    "organizationId" to organizationId,
                    "cardNumber" to "GQL-${System.nanoTime()}",
                    "fullName" to fullName
                )
            )
        )
        ensureNoGraphqlErrors(response)
        val id = response.path("data").path("createPatient").path("id").asText()
        runCatching { createdPatientIds += UUID.fromString(id) }
        return id
    }

    fun createAppointmentViaRest(
        patientId: String,
        employeeId: String,
        branchId: String,
        roomId: String
    ): String {
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "patientId" to patientId,
                "employeeId" to employeeId,
                "timeSlotId" to null,
                "branchId" to branchId,
                "roomId" to roomId,
                "status" to "SCHEDULED",
                "source" to "MANUAL",
                "notes" to "graphql status test",
                "createdBy" to employeeId
            )
        )
        val request = HttpRequest.POST("/api/appointments", payload).contentType(MediaType.APPLICATION_JSON)
        val body = try {
            client.toBlocking().retrieve(request)
        } catch (e: HttpClientResponseException) {
            val responseBody = e.response.getBody(String::class.java).orElse("")
            throw AssertionError("Could not create appointment via REST: status=${e.status}, body=$responseBody", e)
        }
        val id = objectMapper.readTree(body).path("id").asText()
        runCatching { createdAppointmentIds += UUID.fromString(id) }
        return id
    }

    fun ensureOrganizationId(): UUID {
        val existing = organizationRepository.findAllOrdered().firstOrNull()
        if (existing != null) return existing.id
        val now = Instant.now()
        val org = OrganizationEntity(
            id = UUID.randomUUID(),
            name = "GraphQL Test Organization",
            codeOkpo = "GQL-OKPO-${System.nanoTime()}",
            codeOkud = "GQL-OKUD",
            address = "Test address",
            createdAt = now
        )
        organizationRepository.save(org)
        createdOrganizationIds += org.id
        return org.id
    }

    fun ensureEmployeeId(): UUID {
        val existing = employeeRepository.findAllOrdered().firstOrNull()
        if (existing != null) return existing.id
        val now = Instant.now()
        val employee = EmployeeEntity(
            id = UUID.randomUUID(),
            fullName = "GraphQL Test Employee",
            email = "gql-${System.nanoTime()}@example.test",
            phone = "+10000000000",
            passwordHash = "test-password-hash",
            isActive = true,
            digitalSignature = null,
            createdAt = now,
            updatedAt = now
        )
        employeeRepository.save(employee)
        createdEmployeeIds += employee.id
        return employee.id
    }

    fun ensureBranchId(organizationId: UUID): UUID {
        val existing = branchRepository.findAllOrdered().firstOrNull()
        if (existing != null) return existing.id
        val branch = BranchEntity(
            id = UUID.randomUUID(),
            organizationId = organizationId,
            name = "GraphQL Test Branch",
            address = "Test branch address",
            phone = "+10000000001",
            isActive = true,
            createdAt = Instant.now()
        )
        branchRepository.save(branch)
        createdBranchIds += branch.id
        return branch.id
    }

    fun ensureRoomId(branchId: UUID): UUID {
        val existing = roomRepository.findAllOrdered().firstOrNull()
        if (existing != null) return existing.id
        val room = RoomEntity(
            id = UUID.randomUUID(),
            branchId = branchId,
            name = "101",
            description = "GraphQL test room",
            isActive = true
        )
        roomRepository.save(room)
        createdRoomIds += room.id
        return room.id
    }

    fun cleanupCreatedTestData() {
        createdAppointmentIds.forEach { id -> runCatching { appointmentRepository.deleteById(id) } }
        createdAppointmentIds.clear()

        createdPatientIds.forEach { id -> runCatching { patientRepository.deleteById(id) } }
        createdPatientIds.clear()

        createdRoomIds.forEach { id -> runCatching { roomRepository.deleteById(id) } }
        createdRoomIds.clear()

        createdBranchIds.forEach { id -> runCatching { branchRepository.deleteById(id) } }
        createdBranchIds.clear()

        createdEmployeeIds.forEach { id -> runCatching { employeeRepository.deleteById(id) } }
        createdEmployeeIds.clear()

        createdOrganizationIds.forEach { id -> runCatching { organizationRepository.deleteById(id) } }
        createdOrganizationIds.clear()
    }

    afterSpec {
        cleanupCreatedTestData()
    }

    "POST /graphql patients query returns paginated payload" {
        val query = """
            query Patients(${'$'}page: Int!, ${'$'}size: Int!) {
              patients(page: ${'$'}page, size: ${'$'}size) {
                pageInfo { page size totalItems totalPages hasNext hasPrevious }
                items { id fullName }
              }
            }
        """.trimIndent()

        val response = graphQl(query, mapOf("page" to 0, "size" to 2))
        ensureNoGraphqlErrors(response)
        response.path("data").path("patients").path("pageInfo").path("size").asInt() shouldBe 2
        response.path("data").path("patients").path("items").isArray shouldBe true
    }

    "POST /graphql resolves nested Patient.appointments field" {
        val orgId = ensureOrganizationId()
        val patientId = createPatientViaGraphql(orgId.toString(), "GraphQL Resolver Test")

        val query = """
            query PatientAppointments(${'$'}id: ID!) {
              patient(id: ${'$'}id) {
                id
                appointments(page: 0, size: 3) {
                  pageInfo { page size totalItems totalPages hasNext hasPrevious }
                  items { id status }
                }
              }
            }
        """.trimIndent()

        val response = graphQl(query, mapOf("id" to patientId))
        ensureNoGraphqlErrors(response)
        response.path("data").path("patient").path("id").asText() shouldBe patientId
        response.path("data").path("patient").path("appointments").path("pageInfo").path("size").asInt() shouldBe 3
    }

    "POST /graphql confirmAppointment mutation updates status" {
        val organizationId = ensureOrganizationId()
        val employeeId = ensureEmployeeId()
        val branchId = ensureBranchId(organizationId)
        val roomId = ensureRoomId(branchId)

        val patientId = createPatientViaGraphql(organizationId.toString(), "GraphQL Confirm Test")
        val appointmentId = createAppointmentViaRest(
            patientId = patientId,
            employeeId = employeeId.toString(),
            branchId = branchId.toString(),
            roomId = roomId.toString()
        )

        val mutation = """
            mutation Confirm(${'$'}id: ID!) {
              confirmAppointment(appointmentId: ${'$'}id) {
                id
                status
              }
            }
        """.trimIndent()

        val response = graphQl(mutation, mapOf("id" to appointmentId))
        ensureNoGraphqlErrors(response)
        response.path("data").path("confirmAppointment").path("id").asText() shouldBe appointmentId
        response.path("data").path("confirmAppointment").path("status").asText() shouldBe "CONFIRMED"
    }

    "POST /graphql returns error when query complexity is too high" {
        val repeated = (1..30).joinToString("\n") { idx ->
            """
            q$idx: patients(page: 0, size: 1) {
              items {
                id
                fullName
                appointments(page: 0, size: 1) {
                  items { id status }
                }
              }
            }
            """.trimIndent()
        }
        val query = "query TooComplex {\n$repeated\n}"

        val response = graphQl(query)
        response.path("errors").isArray shouldBe true
        val errors = response.path("errors")
        val msg = if (errors.isArray && errors.size() > 0) {
            errors.get(0).path("message").asText().lowercase()
        } else {
            ""
        }
        msg shouldContain "complex"
    }

    "POST /graphql all-entities catalog query works for missing-from-old-schema type" {
        val query = """
            query {
              allSystemSettingEntities { id }
              allInventoryOperationEntities { id }
            }
        """.trimIndent()
        val response = graphQl(query)
        ensureNoGraphqlErrors(response)
        response.path("data").path("allSystemSettingEntities").isArray shouldBe true
        response.path("data").path("allInventoryOperationEntities").isArray shouldBe true
    }
})
