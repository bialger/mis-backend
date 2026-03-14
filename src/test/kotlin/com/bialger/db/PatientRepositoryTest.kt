package com.bialger.db

import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.entity.PatientEntity
import com.bialger.db.repository.OrganizationRepository
import com.bialger.db.repository.PatientRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.UUID

@MicronautTest(transactional = true)
class PatientRepositoryTest(
    private val patientRepository: PatientRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    "save and findById" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Клиника", codeOkpo = "111", address = "A"))

        val entity = PatientEntity(
            id = UUID.randomUUID(),
            cardNumber = "CARD-001",
            organizationId = orgId,
            fullName = "Иванов Иван"
        )
        patientRepository.save(entity)

        val found = patientRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.fullName shouldBe "Иванов Иван"
        found.cardNumber shouldBe "CARD-001"
    }

    "findByCardNumber" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Орг", codeOkpo = "222", address = "B"))
        val patientId = UUID.randomUUID()
        patientRepository.save(
            PatientEntity(id = patientId, cardNumber = "CARD-002", organizationId = orgId, fullName = "Петров")
        )

        val found = patientRepository.findByCardNumber("CARD-002")
        found.shouldNotBeNull()
        found.id shouldBe patientId
    }

    "findByOrganizationId" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Центр", codeOkpo = "333", address = "C"))

        patientRepository.save(
            PatientEntity(id = UUID.randomUUID(), cardNumber = "C1", organizationId = orgId, fullName = "Patient 1")
        )
        patientRepository.save(
            PatientEntity(id = UUID.randomUUID(), cardNumber = "C2", organizationId = orgId, fullName = "Patient 2")
        )

        val byOrg = patientRepository.findByOrganizationId(orgId)
        byOrg shouldHaveSize 2
    }

    "existsByCardNumber" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Орг", codeOkpo = "444", address = "D"))
        patientRepository.save(
            PatientEntity(id = UUID.randomUUID(), cardNumber = "EXISTS-CARD", organizationId = orgId, fullName = "X")
        )

        patientRepository.existsByCardNumber("EXISTS-CARD") shouldBe true
        patientRepository.existsByCardNumber("NO-SUCH-CARD") shouldBe false
    }
})
