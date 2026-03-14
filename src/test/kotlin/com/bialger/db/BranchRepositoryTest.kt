package com.bialger.db

import com.bialger.db.entity.BranchEntity
import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.repository.BranchRepository
import com.bialger.db.repository.OrganizationRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.UUID

@MicronautTest(transactional = true)
class BranchRepositoryTest(
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    "save and find by organization" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Организация"))

        val branch = BranchEntity(
            id = UUID.randomUUID(),
            organizationId = orgId,
            name = "Филиал Центр",
            isActive = true
        )
        branchRepository.save(branch)

        val found = branchRepository.findById(branch.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Филиал Центр"
        found.organizationId shouldBe orgId

        val byOrg = branchRepository.findByOrganizationId(orgId)
        byOrg shouldHaveSize 1
        byOrg.first().name shouldBe "Филиал Центр"
    }
})
