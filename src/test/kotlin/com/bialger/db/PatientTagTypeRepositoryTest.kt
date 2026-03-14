package com.bialger.db

import com.bialger.db.entity.PatientTagTypeEntity
import com.bialger.db.repository.PatientTagTypeRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class PatientTagTypeRepositoryTest(
    private val repository: PatientTagTypeRepository
) : StringSpec({

    "save and findById" {
        val entity = PatientTagTypeEntity(
            id = UUID.randomUUID(),
            code = "VIP",
            icon = "star",
            name = "VIP Patient",
            description = "Important patient",
            isActive = true
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.code shouldBe "VIP"
        found.name shouldBe "VIP Patient"
    }

    "findByCode" {
        val id = UUID.randomUUID()
        repository.save(PatientTagTypeEntity(id = id, code = "CHRONIC", name = "Chronic", isActive = true))

        val found = repository.findByCode("CHRONIC")
        found.shouldNotBeNull()
        found.id shouldBe id
    }

    "findByIsActive" {
        repository.save(PatientTagTypeEntity(UUID.randomUUID(), "A1", name = "Active1", isActive = true))
        repository.save(PatientTagTypeEntity(UUID.randomUUID(), "A2", name = "Active2", isActive = true))
        repository.save(PatientTagTypeEntity(UUID.randomUUID(), "I1", name = "Inactive", isActive = false))

        val active = repository.findByIsActive(true)
        active shouldHaveSize 2
    }
})
