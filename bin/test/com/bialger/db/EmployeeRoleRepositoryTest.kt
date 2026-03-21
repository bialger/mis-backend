package com.bialger.db

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.RoleEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.RoleRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class EmployeeRoleRepositoryTest(
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val employeeRepository: EmployeeRepository,
    private val roleRepository: RoleRepository
) : StringSpec({

    "save and findByEmployeeId" {
        val employeeId = UUID.randomUUID()
        val roleId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = employeeId, fullName = "E", email = "e@test.mis", passwordHash = "x", isActive = true))
        roleRepository.save(RoleEntity(id = roleId, name = "DOCTOR", displayName = "Doctor"))

        employeeRoleRepository.save(employeeId, roleId)

        val links = employeeRoleRepository.findByEmployeeId(employeeId)
        links shouldHaveSize 1
        links.first().employeeId shouldBe employeeId
        links.first().roleId shouldBe roleId
    }

    "findByRoleId" {
        val roleId = UUID.randomUUID()
        roleRepository.save(RoleEntity(id = roleId, name = "NURSE", displayName = "Nurse"))
        val emp1 = UUID.randomUUID()
        val emp2 = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = emp1, fullName = "E1", email = "e1@test.mis", passwordHash = "x", isActive = true))
        employeeRepository.save(EmployeeEntity(id = emp2, fullName = "E2", email = "e2@test.mis", passwordHash = "x", isActive = true))
        employeeRoleRepository.save(emp1, roleId)
        employeeRoleRepository.save(emp2, roleId)

        val links = employeeRoleRepository.findByRoleId(roleId)
        links shouldHaveSize 2
    }

    "deleteByEmployeeIdAndRoleId" {
        val employeeId = UUID.randomUUID()
        val roleId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = employeeId, fullName = "E", email = "e3@test.mis", passwordHash = "x", isActive = true))
        roleRepository.save(RoleEntity(id = roleId, name = "ADMIN"))
        employeeRoleRepository.save(employeeId, roleId)

        employeeRoleRepository.deleteByEmployeeIdAndRoleId(employeeId, roleId)

        employeeRoleRepository.findByEmployeeId(employeeId) shouldHaveSize 0
    }
})
