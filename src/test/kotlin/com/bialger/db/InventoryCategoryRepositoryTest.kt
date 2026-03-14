package com.bialger.db

import com.bialger.db.entity.InventoryCategoryEntity
import com.bialger.db.repository.InventoryCategoryRepository
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class InventoryCategoryRepositoryTest(
    private val repository: InventoryCategoryRepository
) : StringSpec({

    "save and findById" {
        val entity = InventoryCategoryEntity(
            id = UUID.randomUUID(),
            name = "Medications",
            description = "All medications"
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Medications"
    }

    "findByName" {
        val id = UUID.randomUUID()
        repository.save(InventoryCategoryEntity(id = id, name = "Consumables"))

        val found = repository.findByName("Consumables")
        found.shouldNotBeNull()
        found.id shouldBe id
    }

    "findByName returns null when not found" {
        repository.findByName("Nonexistent").shouldBeNull()
    }
})
