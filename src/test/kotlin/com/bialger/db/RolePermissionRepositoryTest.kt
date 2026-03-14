package com.bialger.db

import com.bialger.db.entity.PermissionEntity
import com.bialger.db.entity.RoleEntity
import com.bialger.db.repository.PermissionRepository
import com.bialger.db.repository.RolePermissionRepository
import com.bialger.db.repository.RoleRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class RolePermissionRepositoryTest(
    private val rolePermissionRepository: RolePermissionRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository
) : StringSpec({

    "save and findByRoleId" {
        val roleId = UUID.randomUUID()
        val permissionId = UUID.randomUUID()
        roleRepository.save(RoleEntity(id = roleId, name = "DOCTOR"))
        permissionRepository.save(PermissionEntity(id = permissionId, code = "patient.view"))

        rolePermissionRepository.save(roleId, permissionId)

        val links = rolePermissionRepository.findByRoleId(roleId)
        links shouldHaveSize 1
        links.first().roleId shouldBe roleId
        links.first().permissionId shouldBe permissionId
    }

    "findByPermissionId" {
        val permissionId = UUID.randomUUID()
        permissionRepository.save(PermissionEntity(id = permissionId, code = "appointment.edit"))
        val role1 = UUID.randomUUID()
        val role2 = UUID.randomUUID()
        roleRepository.save(RoleEntity(id = role1, name = "DOCTOR"))
        roleRepository.save(RoleEntity(id = role2, name = "ADMIN"))
        rolePermissionRepository.save(role1, permissionId)
        rolePermissionRepository.save(role2, permissionId)

        val links = rolePermissionRepository.findByPermissionId(permissionId)
        links shouldHaveSize 2
    }

    "deleteByRoleIdAndPermissionId" {
        val roleId = UUID.randomUUID()
        val permissionId = UUID.randomUUID()
        roleRepository.save(RoleEntity(id = roleId, name = "NURSE"))
        permissionRepository.save(PermissionEntity(id = permissionId, code = "record.view"))
        rolePermissionRepository.save(roleId, permissionId)

        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId)

        rolePermissionRepository.findByRoleId(roleId) shouldHaveSize 0
    }
})
