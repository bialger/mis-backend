package com.bialger.db

import com.bialger.db.entity.BranchEntity
import com.bialger.db.entity.OrganizationEntity
import com.bialger.db.entity.ServiceEntity
import com.bialger.db.repository.BranchRepository
import com.bialger.db.repository.OrganizationRepository
import com.bialger.db.repository.ServiceRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.math.BigDecimal
import java.util.UUID

@MicronautTest(transactional = true)
class ServiceRepositoryTest(
    private val serviceRepository: ServiceRepository,
    private val branchRepository: BranchRepository,
    private val organizationRepository: OrganizationRepository
) : StringSpec({

    "save and findById" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))

        val entity = ServiceEntity(
            id = UUID.randomUUID(),
            name = "Consultation",
            price = BigDecimal("100.00"),
            costPrice = BigDecimal("50.00"),
            branchId = branchId,
            isActive = true
        )
        serviceRepository.save(entity)

        val found = serviceRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.name shouldBe "Consultation"
        found.price shouldBe BigDecimal("100.00")
    }

    "findByBranchId" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "2", address = "B"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))

        serviceRepository.save(ServiceEntity(id = UUID.randomUUID(), name = "S1", price = BigDecimal.ZERO, branchId = branchId))
        serviceRepository.save(ServiceEntity(id = UUID.randomUUID(), name = "S2", price = BigDecimal.ZERO, branchId = branchId))

        val list = serviceRepository.findByBranchId(branchId)
        list shouldHaveSize 2
    }

    "findByIsActive" {
        val entity = ServiceEntity(id = UUID.randomUUID(), name = "Active", price = BigDecimal.ZERO, isActive = true)
        serviceRepository.save(entity)
        serviceRepository.save(ServiceEntity(id = UUID.randomUUID(), name = "Inactive", price = BigDecimal.ZERO, isActive = false))

        val active = serviceRepository.findByIsActive(true)
        active.any { it.name == "Active" } shouldBe true
    }
})
