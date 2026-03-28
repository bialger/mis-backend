package com.bialger.db

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.EmployeePermissionEntity
import com.bialger.domain.core.entity.PermissionEntity
import com.bialger.domain.core.repository.EmployeePermissionRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.PermissionRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class EmployeePermissionRepositoryTest(
    private val employeePermissionRepository: EmployeePermissionRepository,
    private val employeeRepository: EmployeeRepository,
    private val permissionRepository: PermissionRepository
) : StringSpec({

    "save and findById" {
        val employeeId = UUID.randomUUID()
        val permissionId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = employeeId, fullName = "E", email = "e@test.mis", passwordHash = "x", isActive = true))
        permissionRepository.save(PermissionEntity(id = permissionId, code = "override.permission"))

        val entity = EmployeePermissionEntity(
            id = UUID.randomUUID(),
            employeeId = employeeId,
            permissionId = permissionId,
            isGranted = true
        )
        employeePermissionRepository.save(entity)

        val found = employeePermissionRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.employeeId shouldBe employeeId
        found.permissionId shouldBe permissionId
        found.isGranted shouldBe true
    }

    "findByEmployeeId" {
        val employeeId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = employeeId, fullName = "E", email = "e2@test.mis", passwordHash = "x", isActive = true))
        val perm1 = UUID.randomUUID()
        val perm2 = UUID.randomUUID()
        permissionRepository.save(PermissionEntity(id = perm1, code = "p1"))
        permissionRepository.save(PermissionEntity(id = perm2, code = "p2"))
        employeePermissionRepository.save(EmployeePermissionEntity(UUID.randomUUID(), employeeId, perm1, true))
        employeePermissionRepository.save(EmployeePermissionEntity(UUID.randomUUID(), employeeId, perm2, false))

        val list = employeePermissionRepository.findByEmployeeId(employeeId)
        list shouldHaveSize 2
    }

    "findByPermissionId" {
        val permissionId = UUID.randomUUID()
        permissionRepository.save(PermissionEntity(id = permissionId, code = "shared.perm"))
        val empId = UUID.randomUUID()
        employeeRepository.save(EmployeeEntity(id = empId, fullName = "E", email = "e3@test.mis", passwordHash = "x", isActive = true))
        employeePermissionRepository.save(EmployeePermissionEntity(UUID.randomUUID(), empId, permissionId, true))

        val list = employeePermissionRepository.findByPermissionId(permissionId)
        list shouldHaveSize 1
        list.first().isGranted shouldBe true
    }
})
