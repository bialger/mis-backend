package com.bialger.db

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.SpecialtyEntity
import com.bialger.domain.clinical.entity.TemplateEntity
import com.bialger.domain.clinical.enums.TemplateType
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.SpecialtyRepository
import com.bialger.domain.clinical.repository.TemplateRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class TemplateRepositoryTest(
    private val templateRepository: TemplateRepository,
    private val specialtyRepository: SpecialtyRepository,
    private val employeeRepository: EmployeeRepository
) : StringSpec({

    "save and findById" {
        val entity = TemplateEntity(
            id = UUID.randomUUID(),
            name = "Default examination",
            type = TemplateType.STANDARD,
            content = "Template body",
            isActive = true
        )
        templateRepository.save(entity)

        val found = templateRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Default examination"
        found.type shouldBe TemplateType.STANDARD
    }

    "findBySpecialtyId" {
        val specialtyId = UUID.randomUUID()
        specialtyRepository.save(SpecialtyEntity(id = specialtyId, name = "Cardiology"))
        templateRepository.save(TemplateEntity(UUID.randomUUID(), "T1", TemplateType.STANDARD, specialtyId = specialtyId, content = "C1"))
        templateRepository.save(TemplateEntity(UUID.randomUUID(), "T2", TemplateType.CLINICAL_GUIDELINE, specialtyId = specialtyId, content = "C2"))

        val list = templateRepository.findBySpecialtyId(specialtyId)
        list shouldHaveSize 2
    }

    "findByEmployeeId" {
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "D", email = "d@test.mis", passwordHash = "x", isActive = true))
        templateRepository.save(TemplateEntity(UUID.randomUUID(), "Personal", TemplateType.PERSONAL, employeeId = empId, content = "Personal template"))

        val list = templateRepository.findByEmployeeId(empId)
        list shouldHaveSize 1
        list.first().type shouldBe TemplateType.PERSONAL
    }

    "findByIsActive" {
        templateRepository.save(TemplateEntity(UUID.randomUUID(), "Active", TemplateType.STANDARD, content = "C", isActive = true))
        templateRepository.save(TemplateEntity(UUID.randomUUID(), "Inactive", TemplateType.STANDARD, content = "C", isActive = false))

        val active = templateRepository.findByIsActive(true)
        active.any { it.name == "Active" } shouldBe true
    }
})
