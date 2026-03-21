package com.bialger.db

import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.core.repository.OrganizationRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.UUID

@MicronautTest(transactional = true)
class OrganizationRepositoryTest(
    private val repository: OrganizationRepository
) : StringSpec({

    "save and findById" {
        val entity = OrganizationEntity(
            id = UUID.randomUUID(),
            name = "Тестовая клиника",
            codeOkpo = "12345678",
            address = "ул. Тестовая, 1"
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Тестовая клиника"
        found.codeOkpo shouldBe "12345678"
    }

    "findAll returns saved organizations" {
        val id = UUID.randomUUID()
        repository.save(OrganizationEntity(id = id, name = "Медцентр №2"))

        val mine = repository.findAll().filter { it.id == id }
        mine shouldHaveSize 1
        mine.first().name shouldBe "Медцентр №2"
    }

    "update existing organization" {
        val id = UUID.randomUUID()
        repository.save(OrganizationEntity(id = id, name = "Было"))
        repository.update(OrganizationEntity(id = id, name = "Стало"))

        val found = requireNotNull(repository.findById(id).orElse(null))
        found.name shouldBe "Стало"
    }

    "delete removes organization" {
        val id = UUID.randomUUID()
        repository.save(OrganizationEntity(id = id, name = "На удаление"))
        repository.deleteById(id)

        repository.findById(id).orElse(null).shouldBeNull()
    }
})
