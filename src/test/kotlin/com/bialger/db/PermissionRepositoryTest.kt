package com.bialger.db

import com.bialger.domain.core.entity.PermissionEntity
import com.bialger.domain.core.repository.PermissionRepository
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class PermissionRepositoryTest(
    private val permissionRepository: PermissionRepository
) : StringSpec({

    "save and findById" {
        val entity = PermissionEntity(
            id = UUID.randomUUID(),
            code = "patient.view",
            name = "View patients",
            description = "View patient data"
        )
        permissionRepository.save(entity)

        val found = permissionRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.code shouldBe "patient.view"
        found.name shouldBe "View patients"
    }

    "findByCode" {
        val id = UUID.randomUUID()
        permissionRepository.save(PermissionEntity(id = id, code = "appointment.create"))

        val found = permissionRepository.findByCode("appointment.create")
        found.shouldNotBeNull()
        found.id shouldBe id
    }

    "findByCode returns null when not found" {
        permissionRepository.findByCode("nonexistent.permission").shouldBeNull()
    }
})
