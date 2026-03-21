package com.bialger.domain.patient.mvc

import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.enums.GenderType
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Singleton
class PatientMvcService(
    private val patientRepository: PatientRepository,
    private val organizationRepository: OrganizationRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<PatientEntity> = patientRepository.findAllOrdered()

    fun getById(id: UUID): PatientEntity? = patientRepository.findById(id).orElse(null)

    fun create(
        organizationId: UUID,
        cardNumber: String,
        fullName: String,
        gender: GenderType?,
        birthDate: LocalDate?,
        phone: String?,
        email: String?
    ): PatientEntity {
        require(organizationRepository.findById(organizationId).isPresent) { "Организация не найдена" }
        val card = cardNumber.trim()
        require(card.isNotEmpty()) { "Укажите номер карты" }
        assertCardNumberFree(card, null)
        val name = fullName.trim()
        require(name.isNotEmpty()) { "Укажите ФИО" }
        val now = Instant.now()
        val id = UUID.randomUUID()
        val entity = PatientEntity(
            id = id,
            cardNumber = card,
            organizationId = organizationId,
            fullName = name,
            gender = gender,
            birthDate = birthDate,
            registrationAddress = null,
            residenceAddress = null,
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            email = email?.trim()?.takeIf { it.isNotEmpty() },
            localityType = null,
            citizenship = null,
            identityDocument = null,
            omsPolicy = null,
            snils = null,
            insuranceOrganization = null,
            contactPerson = null,
            guardian = null,
            profession = null,
            workplace = null,
            createdAt = now,
            updatedAt = now
        )
        patientRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.fullName)
        return entity
    }

    fun update(
        id: UUID,
        organizationId: UUID,
        cardNumber: String,
        fullName: String,
        gender: GenderType?,
        birthDate: LocalDate?,
        phone: String?,
        email: String?
    ): PatientEntity {
        require(organizationRepository.findById(organizationId).isPresent) { "Организация не найдена" }
        val existing = patientRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val card = cardNumber.trim()
        require(card.isNotEmpty()) { "Укажите номер карты" }
        assertCardNumberFree(card, id)
        val name = fullName.trim()
        require(name.isNotEmpty()) { "Укажите ФИО" }
        val updated = existing.copy(
            organizationId = organizationId,
            cardNumber = card,
            fullName = name,
            gender = gender,
            birthDate = birthDate,
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            email = email?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = Instant.now()
        )
        patientRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.fullName)
        return updated
    }

    fun delete(id: UUID) {
        val existing = patientRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        patientRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.fullName)
    }

    private fun assertCardNumberFree(cardNumber: String, excludeId: UUID?) {
        val found = patientRepository.findByCardNumber(cardNumber)
        if (found != null && found.id != excludeId) {
            throw IllegalArgumentException("Номер карты уже занят")
        }
    }

    companion object {
        const val TOPIC = "patients"
        const val EVENT_NAME = "patients-change"

        fun parseGender(raw: String): GenderType? {
            val t = raw.trim().uppercase()
            if (t.isEmpty()) return null
            return when (t) {
                "M" -> GenderType.M
                "F" -> GenderType.F
                else -> throw IllegalArgumentException("Выберите пол или оставьте пустым")
            }
        }

        fun parseBirthDate(raw: String): LocalDate? {
            val t = raw.trim()
            if (t.isEmpty()) return null
            return LocalDate.parse(t)
        }
    }
}
