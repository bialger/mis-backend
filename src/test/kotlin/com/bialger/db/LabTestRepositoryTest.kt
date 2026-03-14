package com.bialger.db

import com.bialger.db.entity.LabTestEntity
import com.bialger.db.entity.LaboratoryEntity
import com.bialger.db.repository.LabTestRepository
import com.bialger.db.repository.LaboratoryRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.math.BigDecimal
import java.util.UUID

@MicronautTest(transactional = true)
class LabTestRepositoryTest(
    private val labTestRepository: LabTestRepository,
    private val laboratoryRepository: LaboratoryRepository
) : StringSpec({

    "save and findById" {
        val labId = UUID.randomUUID()
        laboratoryRepository.save(LaboratoryEntity(id = labId, name = "Lab", isActive = true))

        val entity = LabTestEntity(
            id = UUID.randomUUID(),
            name = "Blood count",
            description = "CBC",
            price = BigDecimal("50.00"),
            laboratoryId = labId,
            isActive = true
        )
        labTestRepository.save(entity)

        val found = labTestRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Blood count"
        found.price shouldBe BigDecimal("50.00")
    }

    "findByLaboratoryId" {
        val labId = UUID.randomUUID()
        laboratoryRepository.save(LaboratoryEntity(id = labId, name = "Lab", isActive = true))
        labTestRepository.save(LabTestEntity(UUID.randomUUID(), "Test1", laboratoryId = labId))
        labTestRepository.save(LabTestEntity(UUID.randomUUID(), "Test2", laboratoryId = labId))

        val list = labTestRepository.findByLaboratoryId(labId)
        list shouldHaveSize 2
    }

    "findByIsActive" {
        labTestRepository.save(LabTestEntity(UUID.randomUUID(), "Active", isActive = true))
        labTestRepository.save(LabTestEntity(UUID.randomUUID(), "Inactive", isActive = false))

        val active = labTestRepository.findByIsActive(true)
        active.any { it.name == "Active" } shouldBe true
    }
})
