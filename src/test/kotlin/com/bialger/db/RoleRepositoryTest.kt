package com.bialger.db

import com.bialger.domain.core.entity.RoleEntity
import com.bialger.domain.core.repository.RoleRepository
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class RoleRepositoryTest(
    private val repository: RoleRepository
) : StringSpec({

    "save and findById" {
        val uniqueName = "ROLE_TEST_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val entity = RoleEntity(
            id = UUID.randomUUID(),
            name = uniqueName,
            displayName = "Doctor",
            description = "Medical staff"
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe uniqueName
        found.displayName shouldBe "Doctor"
    }

    "findByName" {
        val id = UUID.randomUUID()
        val uniqueName = "ADMIN_TEST_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        repository.save(RoleEntity(id = id, name = uniqueName))

        val found = repository.findByName(uniqueName)
        found.shouldNotBeNull()
        found.id shouldBe id
    }

    "findByName returns null when not found" {
        repository.findByName("NONEXISTENT").shouldBeNull()
    }
})
