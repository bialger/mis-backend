package com.bialger.api

import com.bialger.domain.inventory.repository.InventoryItemRepository
import com.bialger.domain.inventory.repository.InventoryOperationRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest
class InventoryOperationsApiTest(
    @param:Client("/") private val client: HttpClient,
    private val objectMapper: ObjectMapper,
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryOperationRepository: InventoryOperationRepository
) : StringSpec({

    val createdItemIds = mutableListOf<UUID>()

    afterTest {
        createdItemIds.forEach { itemId ->
            runCatching {
                inventoryOperationRepository.findByItemId(itemId)
                    .forEach { inventoryOperationRepository.deleteById(it.id) }
            }
            runCatching { inventoryItemRepository.deleteById(itemId) }
        }
        createdItemIds.clear()
    }

    /**
     * Creates an inventory item using seeded branch and category via HTTP API.
     * Returns the item ID string, or "skip" if prerequisites are missing.
     */
    fun createInventoryItem(): String {
        val branchResp = client.toBlocking().retrieve("/api/branches?page=0&size=1")
        val branches = objectMapper.readTree(branchResp).path("content")
        val branchId = if (branches.isArray && branches.size() > 0) branches[0].path("id").asText() else return "skip"

        val existingItems = client.toBlocking().retrieve("/api/inventory-items?page=0&size=1")
        val firstItem = objectMapper.readTree(existingItems).path("content")

        val catId = if (firstItem.isArray && firstItem.size() > 0) {
            firstItem[0].path("categoryId").asText()
        } else {
            return "skip"
        }

        val itemName = "TestItem-${UUID.randomUUID().toString().take(8)}"
        val body = """
            {
              "name":"$itemName",
              "branchId":"$branchId",
              "categoryId":"$catId",
              "unit":"pcs",
              "quantity":"50",
              "minQuantity":"5"
            }
        """.trimIndent()
        val resp = client.toBlocking().retrieve(
            HttpRequest.POST("/api/inventory-items", body).contentType(MediaType.APPLICATION_JSON)
        )
        val id = objectMapper.readTree(resp).path("id").asText()
        runCatching { createdItemIds += UUID.fromString(id) }
        return id
    }

    fun employeeId(): String {
        val resp = client.toBlocking().retrieve("/api/employees?page=0&size=1")
        val content = objectMapper.readTree(resp).path("content")
        return if (content.isArray && content.size() > 0) content[0].path("id").asText() else ""
    }

    "GET /api/inventory-operations returns list" {
        val body = client.toBlocking().retrieve("/api/inventory-operations")
        val tree = objectMapper.readTree(body)
        tree.isArray shouldBe true
    }

    "POST /api/inventory-operations with INCOMING type creates operation" {
        val itemId = createInventoryItem()
        val empId = employeeId()
        if (itemId != "skip" && empId.isNotBlank()) {
            val body = """
                {
                  "itemId":"$itemId",
                  "employeeId":"$empId",
                  "operationType":"INCOMING",
                  "quantity":"10"
                }
            """.trimIndent()
            val resp = client.toBlocking().retrieve(
                HttpRequest.POST("/api/inventory-operations", body).contentType(MediaType.APPLICATION_JSON)
            )
            resp shouldContain "\"operationType\":\"INCOMING\""
        }
    }

    "POST /api/inventory-operations with WRITE_OFF_MANUAL creates operation" {
        val itemId = createInventoryItem()
        val empId = employeeId()
        if (itemId != "skip" && empId.isNotBlank()) {
            val body = """
                {
                  "itemId":"$itemId",
                  "employeeId":"$empId",
                  "operationType":"WRITE_OFF_MANUAL",
                  "quantity":"5",
                  "notes":"Manual write-off"
                }
            """.trimIndent()
            val resp = client.toBlocking().retrieve(
                HttpRequest.POST("/api/inventory-operations", body).contentType(MediaType.APPLICATION_JSON)
            )
            resp shouldContain "\"operationType\":\"WRITE_OFF_MANUAL\""
            resp shouldContain "Manual write-off"
        }
    }

    "POST /api/inventory-operations with WRITE_OFF_AUTO returns 400" {
        val empId = employeeId().takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val body = """
            {
              "itemId":"${UUID.randomUUID()}",
              "employeeId":"$empId",
              "operationType":"WRITE_OFF_AUTO",
              "quantity":"3"
            }
        """.trimIndent()
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/inventory-operations", body).contentType(MediaType.APPLICATION_JSON),
                String::class.java
            )
        }
        ex.status shouldBe HttpStatus.BAD_REQUEST
    }

    "GET /api/inventory-operations/{id} for missing returns 404" {
        val ex = shouldThrow<HttpClientResponseException> {
            client.toBlocking().retrieve("/api/inventory-operations/${UUID.randomUUID()}")
        }
        ex.status shouldBe HttpStatus.NOT_FOUND
    }
})
