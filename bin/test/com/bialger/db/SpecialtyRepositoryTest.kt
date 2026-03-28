package com.bialger.db

import com.bialger.domain.core.entity.SpecialtyEntity
import com.bialger.domain.core.repository.SpecialtyRepository
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
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val n1 = "Therapist_$suffix"
        val n2 = "Surgeon_$suffix"
        repository.save(SpecialtyEntity(UUID.randomUUID(), n1))
        repository.save(SpecialtyEntity(UUID.randomUUID(), n2))

        val all = repository.findAll()
        val mine = all.filter { it.name == n1 || it.name == n2 }
        mine shouldHaveSize 2
    }
})
