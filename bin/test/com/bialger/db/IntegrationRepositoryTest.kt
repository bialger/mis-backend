package com.bialger.db

import com.bialger.domain.attachment.entity.IntegrationEntity
import com.bialger.domain.attachment.enums.IntegrationType
import com.bialger.domain.attachment.repository.IntegrationRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.UUID

@MicronautTest(transactional = true)
class IntegrationRepositoryTest(
    private val repository: IntegrationRepository
) : StringSpec({

    "save and findById" {
        val entity = IntegrationEntity(
            id = UUID.randomUUID(),
            type = IntegrationType.SMS_PROVIDER,
            name = "SMS.ru",
            config = """{"api_key":"secret"}""",
            isActive = true
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "SMS.ru"
        found.type shouldBe IntegrationType.SMS_PROVIDER
    }

    "findByType" {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        repository.save(IntegrationEntity(id = id1, type = IntegrationType.LABORATORY, name = "Лаб 1", isActive = true))
        repository.save(IntegrationEntity(id = id2, type = IntegrationType.LABORATORY, name = "Лаб 2", isActive = true))
        repository.save(IntegrationEntity(id = UUID.randomUUID(), type = IntegrationType.SMS_PROVIDER, name = "SMS", isActive = true))

        val labIntegrations = repository.findByType(IntegrationType.LABORATORY)
        labIntegrations shouldHaveSize 2
        labIntegrations.map { it.name }.toSet() shouldBe setOf("Лаб 1", "Лаб 2")
    }

    "findByIsActive" {
        val id = UUID.randomUUID()
        repository.save(IntegrationEntity(id = id, type = IntegrationType.GOV_SYSTEM, name = "ГИС", isActive = false))
        repository.save(IntegrationEntity(id = UUID.randomUUID(), type = IntegrationType.SMS_PROVIDER, name = "SMS", isActive = true))

        val active = repository.findByIsActive(true)
        active shouldHaveSize 1
        active.first().name shouldBe "SMS"

        val inactive = repository.findByIsActive(false)
        inactive shouldHaveSize 1
        inactive.first().name shouldBe "ГИС"
    }

    "update and delete" {
        val id = UUID.randomUUID()
        repository.save(IntegrationEntity(id = id, type = IntegrationType.LABORATORY, name = "Old", isActive = true))
        repository.update(IntegrationEntity(id = id, type = IntegrationType.LABORATORY, name = "New", isActive = false))

        val found = requireNotNull(repository.findById(id).orElse(null))
        found.name shouldBe "New"
        found.isActive shouldBe false

        repository.deleteById(id)
        repository.findById(id).orElse(null).shouldBeNull()
    }
})
