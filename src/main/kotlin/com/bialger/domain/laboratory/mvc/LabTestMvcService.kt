package com.bialger.domain.laboratory.mvc

import com.bialger.domain.laboratory.entity.LabTestEntity
import com.bialger.domain.laboratory.repository.LabTestRepository
import com.bialger.domain.laboratory.repository.LaboratoryRepository
import com.bialger.domain.mvc.formCheckboxOn
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.util.UUID

data class LabTestListRow(
    val test: LabTestEntity,
    val laboratoryName: String?
)

@Singleton
class LabTestMvcService(
    private val labTestRepository: LabTestRepository,
    private val laboratoryRepository: LaboratoryRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listRows(): List<LabTestListRow> {
        val tests = labTestRepository.findAllOrdered()
        if (tests.isEmpty()) return emptyList()
        val labIds = tests.mapNotNull { it.laboratoryId }.distinct()
        val labNames =
            if (labIds.isEmpty()) emptyMap()
            else laboratoryRepository.findByIds(labIds).associate { it.id to it.name }
        return tests.map { t ->
            LabTestListRow(t, t.laboratoryId?.let { labNames[it] })
        }
    }

    fun getById(id: UUID): LabTestEntity? = labTestRepository.findById(id).orElse(null)

    fun create(
        name: String,
        description: String?,
        price: BigDecimal?,
        laboratoryId: UUID?,
        isActive: Boolean
    ): LabTestEntity {
        val n = name.trim()
        require(n.isNotEmpty())
        if (laboratoryId != null) {
            require(laboratoryRepository.findById(laboratoryId).isPresent) { "Лаборатория не найдена" }
        }
        val id = UUID.randomUUID()
        val entity = LabTestEntity(
            id = id,
            name = n,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            price = price,
            laboratoryId = laboratoryId,
            isActive = isActive
        )
        labTestRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(
        id: UUID,
        name: String,
        description: String?,
        price: BigDecimal?,
        laboratoryId: UUID?,
        isActive: Boolean
    ): LabTestEntity {
        val existing = labTestRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        if (laboratoryId != null) {
            require(laboratoryRepository.findById(laboratoryId).isPresent) { "Лаборатория не найдена" }
        }
        val updated = existing.copy(
            name = n,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            price = price,
            laboratoryId = laboratoryId,
            isActive = isActive
        )
        labTestRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = labTestRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        labTestRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "lab-tests"
        const val EVENT_NAME = "lab-tests-change"

        fun parsePrice(raw: String): BigDecimal? =
            raw.trim().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) }

        fun parseActive(raw: String): Boolean = raw.formCheckboxOn()
    }
}
