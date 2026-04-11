package com.bialger.api

import com.bialger.api.dto.InventoryOperationRestDto
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.inventory.entity.InventoryOperationEntity
import com.bialger.domain.inventory.repository.InventoryItemRepository
import com.bialger.domain.inventory.repository.InventoryOperationRepository
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.serde.annotation.Serdeable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Serdeable
@Introspected
data class InventoryOperationDto(
    val itemId: String,
    val employeeId: String,
    val operationType: String,
    val quantity: String,
    val appointmentId: String? = null,
    val notes: String? = null
)

@Controller("/api/inventory-operations")
@Tag(name = "InventoryOperations", description = "Warehouse movement journal: incoming, write-offs, transfers")
open class InventoryOperationsApiController(
    private val inventoryOperationRepository: InventoryOperationRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val employeeRepository: EmployeeRepository
) {

    @Get(produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "List inventory operations (movement journal)",
        description = "Returns all operations ordered by date. Filter by itemId to get movements for a specific item."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of inventory operations",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = InventoryOperationRestDto::class))])
    )
    fun list(@QueryValue itemId: UUID?): List<InventoryOperationRestDto> {
        val ops = if (itemId != null) {
            inventoryOperationRepository.findByItemIdOrdered(itemId)
        } else {
            inventoryOperationRepository.findAllOrdered()
        }
        val itemNames = inventoryItemRepository.findAllOrdered().associateBy({ it.id }, { it.name })
        val employeeNames = employeeRepository.findAllOrdered().associateBy({ it.id }, { it.fullName })
        return ops.map { toDto(it, itemNames[it.itemId], employeeNames[it.employeeId]) }
    }

    @Get("/{id}", produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Get a single inventory operation by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Inventory operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = InventoryOperationRestDto::class))]),
        ApiResponse(responseCode = "404", description = "Not found")
    )
    fun getOne(@PathVariable id: UUID): InventoryOperationRestDto {
        val op = inventoryOperationRepository.findById(id)
            .orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, "Not found") }
        val itemName = inventoryItemRepository.findById(op.itemId).map { it.name }.orElse(null)
        val employeeName = employeeRepository.findById(op.employeeId).map { it.fullName }.orElse(null)
        return toDto(op, itemName, employeeName)
    }

    @Post(processes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    @Operation(
        summary = "Register an inventory operation (incoming delivery or manual write-off)",
        description = "operationType: INCOMING | WRITE_OFF_MANUAL"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = InventoryOperationRestDto::class))]),
        ApiResponse(responseCode = "400", description = "Validation error or unknown operation type")
    )
    open fun create(@Body dto: InventoryOperationDto): InventoryOperationRestDto {
        val itemId = parseUuid(dto.itemId, "itemId")
        val employeeId = parseUuid(dto.employeeId, "employeeId")
        val qty = runCatching { BigDecimal(dto.quantity) }
            .getOrElse { throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity") }
        require(qty > BigDecimal.ZERO) { "Quantity must be positive" }

        val opType = when (dto.operationType.trim().uppercase()) {
            "INCOMING", "WRITE_OFF_MANUAL" -> dto.operationType.trim().uppercase()
            else -> throw HttpStatusException(HttpStatus.BAD_REQUEST,
                "operationType must be INCOMING or WRITE_OFF_MANUAL; WRITE_OFF_AUTO is system-only")
        }

        if (!inventoryItemRepository.findById(itemId).isPresent) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Inventory item not found")
        }
        if (!employeeRepository.findById(employeeId).isPresent) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Employee not found")
        }

        val appointmentId = dto.appointmentId?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { UUID.fromString(it) }.getOrElse { null } }

        val entity = InventoryOperationEntity(
            id = UUID.randomUUID(),
            itemId = itemId,
            employeeId = employeeId,
            operationType = opType,
            quantity = qty,
            appointmentId = appointmentId,
            notes = dto.notes?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = Instant.now()
        )
        inventoryOperationRepository.save(entity)

        val itemName = inventoryItemRepository.findById(itemId).map { it.name }.orElse(null)
        val employeeName = employeeRepository.findById(employeeId).map { it.fullName }.orElse(null)
        return toDto(entity, itemName, employeeName)
    }

    private fun parseUuid(raw: String, field: String): UUID =
        runCatching { UUID.fromString(raw.trim()) }
            .getOrElse { throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid $field UUID") }

    private fun toDto(
        op: InventoryOperationEntity,
        itemName: String?,
        employeeName: String?
    ): InventoryOperationRestDto = InventoryOperationRestDto(
        id = op.id.toString(),
        itemId = op.itemId.toString(),
        itemName = itemName,
        employeeId = op.employeeId.toString(),
        employeeName = employeeName,
        operationType = op.operationType,
        quantity = op.quantity.toPlainString(),
        appointmentId = op.appointmentId?.toString(),
        notes = op.notes,
        createdAt = op.createdAt?.toString()
    )
}
