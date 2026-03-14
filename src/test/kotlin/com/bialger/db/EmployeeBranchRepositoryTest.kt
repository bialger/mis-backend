package com.bialger.db

import com.bialger.db.entity.BranchEntity
import com.bialger.db.entity.EmployeeEntity
import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.repository.BranchRepository
import com.bialger.db.repository.EmployeeBranchRepository
import com.bialger.db.repository.EmployeeRepository
import com.bialger.db.repository.OrganizationRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class EmployeeBranchRepositoryTest(
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    fun setupOrgAndBranch(): UUID {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))
        return branchId
    }

    "save and findByEmployeeId" {
        val branchId = setupOrgAndBranch()
        val employeeId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = employeeId, fullName = "E", email = "e@test.mis", passwordHash = "x", isActive = true))

        employeeBranchRepository.save(employeeId, branchId)

        val links = employeeBranchRepository.findByEmployeeId(employeeId)
        links shouldHaveSize 1
        links.first().employeeId shouldBe employeeId
        links.first().branchId shouldBe branchId
    }

    "findByBranchId" {
        val branchId = setupOrgAndBranch()
        val emp1 = UUID.randomUUID()
        val emp2 = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = emp1, fullName = "E1", email = "e1@test.mis", passwordHash = "x", isActive = true))
        employeeRepository.save(EmployeeEntity(id = emp2, fullName = "E2", email = "e2@test.mis", passwordHash = "x", isActive = true))
        employeeBranchRepository.save(emp1, branchId)
        employeeBranchRepository.save(emp2, branchId)

        val links = employeeBranchRepository.findByBranchId(branchId)
        links shouldHaveSize 2
    }

    "deleteByEmployeeIdAndBranchId" {
        val branchId = setupOrgAndBranch()
        val employeeId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = employeeId, fullName = "E", email = "e3@test.mis", passwordHash = "x", isActive = true))
        employeeBranchRepository.save(employeeId, branchId)

        employeeBranchRepository.deleteByEmployeeIdAndBranchId(employeeId, branchId)

        employeeBranchRepository.findByEmployeeId(employeeId) shouldHaveSize 0
    }
})
