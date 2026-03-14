package com.bialger.db

import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.entity.PatientEntity
import com.bialger.db.entity.PatientTagEntity
import com.bialger.db.entity.PatientTagTypeEntity
import com.bialger.db.repository.OrganizationRepository
import com.bialger.db.repository.PatientRepository
import com.bialger.db.repository.PatientTagRepository
import com.bialger.db.repository.PatientTagTypeRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class PatientTagRepositoryTest(
    private val patientTagRepository: PatientTagRepository,
    private val patientRepository: PatientRepository,
    private val patientTagTypeRepository: PatientTagTypeRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    fun createPatientAndTagType(): Pair<UUID, UUID> {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val patientId = UUID.randomUUID()
        patientRepository.save(PatientEntity(id = patientId, cardNumber = "C1", organizationId = orgId, fullName = "P"))
        val tagTypeId = UUID.randomUUID()
        patientTagTypeRepository.save(PatientTagTypeEntity(id = tagTypeId, code = "VIP", name = "VIP", isActive = true))
        return patientId to tagTypeId
    }

    "save and findById" {
        val (patientId, tagTypeId) = createPatientAndTagType()
        val entity = PatientTagEntity(
            id = UUID.randomUUID(),
            patientId = patientId,
            tagTypeId = tagTypeId
        )
        patientTagRepository.save(entity)

        val found = patientTagRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.patientId shouldBe patientId
        found.tagTypeId shouldBe tagTypeId
    }

    "findByPatientId" {
        val (patientId, tagTypeId) = createPatientAndTagType()
        patientTagRepository.save(PatientTagEntity(UUID.randomUUID(), patientId, tagTypeId))
        val tagTypeId2 = UUID.randomUUID()
        patientTagTypeRepository.save(PatientTagTypeEntity(id = tagTypeId2, code = "X", name = "X", isActive = true))
        patientTagRepository.save(PatientTagEntity(UUID.randomUUID(), patientId, tagTypeId2))

        val list = patientTagRepository.findByPatientId(patientId)
        list shouldHaveSize 2
    }
})
