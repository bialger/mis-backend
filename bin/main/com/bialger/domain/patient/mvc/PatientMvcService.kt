package com.bialger.domain.patient.mvc

import com.bialger.domain.core.repository.OrganizationRepository
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.enums.GenderType
import com.bialger.domain.patient.enums.LocalityType
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

    fun search(filter: String?): List<PatientEntity> {
        val all = patientRepository.findAllOrdered()
        val q = filter?.trim()?.lowercase() ?: return all
        if (q.isEmpty()) return all
        return all.filter {
            it.fullName.lowercase().contains(q) ||
                (it.phone != null && it.phone.contains(q, ignoreCase = true))
        }
    }

    fun getById(id: UUID): PatientEntity? = patientRepository.findById(id).orElse(null)

    fun create(
        organizationId: UUID,
        cardNumber: String,
        fullName: String,
        gender: GenderType?,
        birthDate: LocalDate?,
        phone: String?,
        email: String?,
        registrationAddress: String?,
        residenceAddress: String?,
        localityType: LocalityType?,
        citizenship: String?,
        identityDocument: String?,
        omsPolicy: String?,
        snils: String?,
        insuranceOrganization: String?,
        contactPerson: String?,
        guardian: String?,
        profession: String?,
        workplace: String?
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
            registrationAddress = registrationAddress?.trim()?.takeIf { it.isNotEmpty() },
            residenceAddress = residenceAddress?.trim()?.takeIf { it.isNotEmpty() },
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            email = email?.trim()?.takeIf { it.isNotEmpty() },
            localityType = localityType,
            citizenship = citizenship?.trim()?.takeIf { it.isNotEmpty() },
            identityDocument = identityDocument?.trim()?.takeIf { it.isNotEmpty() },
            omsPolicy = omsPolicy?.trim()?.takeIf { it.isNotEmpty() },
            snils = snils?.trim()?.takeIf { it.isNotEmpty() },
            insuranceOrganization = insuranceOrganization?.trim()?.takeIf { it.isNotEmpty() },
            contactPerson = contactPerson?.trim()?.takeIf { it.isNotEmpty() },
            guardian = guardian?.trim()?.takeIf { it.isNotEmpty() },
            profession = profession?.trim()?.takeIf { it.isNotEmpty() },
            workplace = workplace?.trim()?.takeIf { it.isNotEmpty() },
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
        email: String?,
        registrationAddress: String?,
        residenceAddress: String?,
        localityType: LocalityType?,
        citizenship: String?,
        identityDocument: String?,
        omsPolicy: String?,
        snils: String?,
        insuranceOrganization: String?,
        contactPerson: String?,
        guardian: String?,
        profession: String?,
        workplace: String?
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
            registrationAddress = registrationAddress?.trim()?.takeIf { it.isNotEmpty() },
            residenceAddress = residenceAddress?.trim()?.takeIf { it.isNotEmpty() },
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            email = email?.trim()?.takeIf { it.isNotEmpty() },
            localityType = localityType,
            citizenship = citizenship?.trim()?.takeIf { it.isNotEmpty() },
            identityDocument = identityDocument?.trim()?.takeIf { it.isNotEmpty() },
            omsPolicy = omsPolicy?.trim()?.takeIf { it.isNotEmpty() },
            snils = snils?.trim()?.takeIf { it.isNotEmpty() },
            insuranceOrganization = insuranceOrganization?.trim()?.takeIf { it.isNotEmpty() },
            contactPerson = contactPerson?.trim()?.takeIf { it.isNotEmpty() },
            guardian = guardian?.trim()?.takeIf { it.isNotEmpty() },
            profession = profession?.trim()?.takeIf { it.isNotEmpty() },
            workplace = workplace?.trim()?.takeIf { it.isNotEmpty() },
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
                "M", "MALE" -> GenderType.M
                "F", "FEMALE" -> GenderType.F
                else -> throw IllegalArgumentException("Выберите пол или оставьте пустым")
            }
        }

        fun parseBirthDate(raw: String): LocalDate? {
            val t = raw.trim()
            if (t.isEmpty()) return null
            return LocalDate.parse(t)
        }

        fun parseLocalityType(raw: String): LocalityType? {
            val t = raw.trim().uppercase()
            if (t.isEmpty()) return null
            return when (t) {
                "URBAN" -> LocalityType.URBAN
                "RURAL" -> LocalityType.RURAL
                else -> throw IllegalArgumentException("Тип местности: Городская или Сельская")
            }
        }
    }
}
