package com.bialger.domain.scheduling.mvc

import com.bialger.domain.mvc.formCheckboxOn
import com.bialger.domain.scheduling.entity.ServiceEntity
import com.bialger.domain.scheduling.repository.ServiceRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.util.UUID

@Singleton
class MedicalServiceMvcService(
    private val serviceRepository: ServiceRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<ServiceEntity> = serviceRepository.findAllOrdered()

    fun getById(id: UUID): ServiceEntity? = serviceRepository.findById(id).orElse(null)

    fun create(name: String, price: BigDecimal, costPrice: BigDecimal?, branchId: UUID?, isActive: Boolean): ServiceEntity {
        val n = name.trim()
        require(n.isNotEmpty())
        val id = UUID.randomUUID()
        val entity = ServiceEntity(
            id = id,
            name = n,
            price = price,
            costPrice = costPrice,
            branchId = branchId,
            isActive = isActive
        )
        serviceRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.name)
        return entity
    }

    fun update(
        id: UUID,
        name: String,
        price: BigDecimal,
        costPrice: BigDecimal?,
        branchId: UUID?,
        isActive: Boolean
    ): ServiceEntity {
        val existing = serviceRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val n = name.trim()
        require(n.isNotEmpty())
        val updated = existing.copy(
            name = n,
            price = price,
            costPrice = costPrice,
            branchId = branchId,
            isActive = isActive
        )
        serviceRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.name)
        return updated
    }

    fun delete(id: UUID) {
        val existing = serviceRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        serviceRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.name)
    }

    companion object {
        const val TOPIC = "medical-services"
        const val EVENT_NAME = "medical-services-change"

        fun parsePrice(raw: String): BigDecimal = BigDecimal(raw.trim())

        fun parseCost(raw: String): BigDecimal? = raw.trim().takeIf { it.isNotEmpty() }?.let { BigDecimal(it) }

        fun parseActiveFlag(raw: String): Boolean = raw.formCheckboxOn()
    }
}
