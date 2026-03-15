package com.bialger.db

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.OrganizationEntity
import com.bialger.domain.system.entity.SystemSettingEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.system.repository.SystemSettingRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.UUID

@MicronautTest(transactional = true)
class SystemSettingRepositoryTest(
    private val systemSettingRepository: SystemSettingRepository,
    private val organizationRepository: OrganizationRepository,
    private val branchRepository: BranchRepository
) : StringSpec({

    "save and findById" {
        val entity = SystemSettingEntity(
            id = UUID.randomUUID(),
            key = "app.name",
            value = "MIS",
            description = "Application name"
        )
        systemSettingRepository.save(entity)

        val found = systemSettingRepository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.key shouldBe "app.name"
        found.value shouldBe "MIS"
    }

    "findByBranchIdIsNull" {
        systemSettingRepository.save(SystemSettingEntity(UUID.randomUUID(), key = "global.setting", value = "true"))

        val list = systemSettingRepository.findByBranchIdIsNull()
        list.any { it.key == "global.setting" } shouldBe true
    }

    "findByBranchIdAndKey" {
        val orgId = UUID.randomUUID()
        organizationRepository.save(OrganizationEntity(id = orgId, name = "Org", codeOkpo = "1", address = "A"))
        val branchId = UUID.randomUUID()
        branchRepository.save(BranchEntity(id = branchId, organizationId = orgId, name = "Branch", isActive = true))
        systemSettingRepository.save(SystemSettingEntity(UUID.randomUUID(), branchId = branchId, key = "branch.timezone", value = "UTC"))

        val found = systemSettingRepository.findByBranchIdAndKey(branchId, "branch.timezone")
        found.shouldNotBeNull()
        found.value shouldBe "UTC"
    }
})
