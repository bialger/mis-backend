package com.bialger.db

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.SpecialtyEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeSpecialtyRepository
import com.bialger.domain.core.repository.SpecialtyRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.UUID

@MicronautTest(transactional = true)
class EmployeeSpecialtyRepositoryTest(
    private val employeeRepository: EmployeeRepository,
    private val specialtyRepository: SpecialtyRepository,
    private val employeeSpecialtyRepository: EmployeeSpecialtyRepository
) : StringSpec({

    "save and findByEmployeeId" {
        val employeeId = UUID.randomUUID()
        val specialtyId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(
                id = employeeId,
                fullName = "Врач",
                email = "doctor${employeeId}@test.mis",
                passwordHash = "hash",
                isActive = true
            )
        )
        specialtyRepository.save(SpecialtyEntity(id = specialtyId, name = "Терапевт"))

        employeeSpecialtyRepository.save(employeeId, specialtyId)

        val links = employeeSpecialtyRepository.findByEmployeeId(employeeId)
        links shouldHaveSize 1
        links.first().employeeId shouldBe employeeId
        links.first().specialtyId shouldBe specialtyId
    }

    "deleteByEmployeeIdAndSpecialtyId" {
        val employeeId = UUID.randomUUID()
        val specialtyId = UUID.randomUUID()
        employeeRepository.save(
            EmployeeEntity(
                id = employeeId,
                fullName = "Врач2",
                email = "doctor2${employeeId}@test.mis",
                passwordHash = "hash",
                isActive = true
            )
        )
        specialtyRepository.save(SpecialtyEntity(id = specialtyId, name = "Ортопед"))
        employeeSpecialtyRepository.save(employeeId, specialtyId)

        employeeSpecialtyRepository.deleteByEmployeeIdAndSpecialtyId(employeeId, specialtyId)

        employeeSpecialtyRepository.findByEmployeeId(employeeId) shouldHaveSize 0
    }
})
