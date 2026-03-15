package com.bialger.db

import com.bialger.domain.patient.enums.ConsentType
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.patient.entity.PatientConsentEntity
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.patient.repository.PatientConsentRepository
import com.bialger.domain.patient.repository.PatientRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.Instant
import java.util.UUID

@MicronautTest(transactional = true)
class PatientConsentRepositoryTest(
    private val patientConsentRepository: PatientConsentRepository,
    private val patientRepository: PatientRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    fun createPatient(): UUID {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val patientId = UUID.randomUUID()
        patientRepository.save(PatientEntity(id = patientId, cardNumber = "C1", organizationId = orgId, fullName = "P"))
        return patientId
    }

    "save and findById" {
        val patientId = createPatient()
        val entity = PatientConsentEntity(
            id = UUID.randomUUID(),
            patientId = patientId,
            consentType = ConsentType.GOV_DATA_TRANSFER,
            isGranted = true,
            grantedAt = Instant.now()
        )
        patientConsentRepository.save(entity)

        val found = patientConsentRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.consentType shouldBe ConsentType.GOV_DATA_TRANSFER
        found.isGranted shouldBe true
    }

    "findByPatientId" {
        val patientId = createPatient()
        patientConsentRepository.save(PatientConsentEntity(UUID.randomUUID(), patientId, ConsentType.GOV_DATA_TRANSFER, true))
        patientConsentRepository.save(PatientConsentEntity(UUID.randomUUID(), patientId, ConsentType.MARKETING, false))

        val list = patientConsentRepository.findByPatientId(patientId)
        list shouldHaveSize 2
    }
})
