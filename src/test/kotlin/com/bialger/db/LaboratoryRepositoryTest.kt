package com.bialger.db

import com.bialger.domain.laboratory.entity.LaboratoryEntity
import com.bialger.domain.laboratory.repository.LaboratoryRepository
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class LaboratoryRepositoryTest(
    private val repository: LaboratoryRepository
) : StringSpec({

    "save and findById" {
        val entity = LaboratoryEntity(
            id = UUID.randomUUID(),
            name = "Lab InVitro",
            integrationType = "LABORATORY",
            config = """{"url":"https://lab.example"}""",
            isActive = true
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Lab InVitro"
        found.isActive shouldBe true
    }
})
