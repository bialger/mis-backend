package com.bialger.db

import com.bialger.db.entity.SpecialtyEntity
import com.bialger.db.repository.SpecialtyRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class SpecialtyRepositoryTest(
    private val repository: SpecialtyRepository
) : StringSpec({

    "save and findById" {
        val entity = SpecialtyEntity(
            id = UUID.randomUUID(),
            name = "Cardiology",
            description = "Heart diseases"
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Cardiology"
        found.description shouldBe "Heart diseases"
    }

    "findAll" {
        repository.save(SpecialtyEntity(UUID.randomUUID(), "Therapist"))
        repository.save(SpecialtyEntity(UUID.randomUUID(), "Surgeon"))

        val all = repository.findAll()
        all shouldHaveSize 2
    }
})
