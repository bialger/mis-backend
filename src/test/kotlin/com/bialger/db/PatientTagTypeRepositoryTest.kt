package com.bialger.db

import com.bialger.domain.patient.entity.PatientTagTypeEntity
import com.bialger.domain.patient.repository.PatientTagTypeRepository
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
        val code = "VIP_TEST_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val entity = PatientTagTypeEntity(
            id = UUID.randomUUID(),
            code = code,
            icon = "star",
            name = "VIP Patient",
            description = "Important patient",
            isActive = true
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.code shouldBe code
        found.name shouldBe "VIP Patient"
    }

    "findByCode" {
        val id = UUID.randomUUID()
        val code = "CHRONIC_TEST_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        repository.save(PatientTagTypeEntity(id = id, code = code, name = "Chronic", isActive = true))

        val found = repository.findByCode(code)
        found.shouldNotBeNull()
        found.id shouldBe id
    }

    "findByIsActive" {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val a1 = "A1_$suffix"
        val a2 = "A2_$suffix"
        val i1 = "I1_$suffix"
        repository.save(PatientTagTypeEntity(UUID.randomUUID(), a1, name = "Active1", isActive = true))
        repository.save(PatientTagTypeEntity(UUID.randomUUID(), a2, name = "Active2", isActive = true))
        repository.save(PatientTagTypeEntity(UUID.randomUUID(), i1, name = "Inactive", isActive = false))

        val active = repository.findByIsActive(true).filter { it.code == a1 || it.code == a2 }
        active shouldHaveSize 2
    }
})
