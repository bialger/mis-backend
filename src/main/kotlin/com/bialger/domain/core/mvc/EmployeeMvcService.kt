package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

/**
 * Демо: пароль сохраняется как введённая строка (без хеширования). В продакшене — только хеш.
 */
@Singleton
class EmployeeMvcService(
    private val employeeRepository: EmployeeRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listAll(): List<EmployeeEntity> = employeeRepository.findAllOrdered()

    fun getById(id: UUID): EmployeeEntity? = employeeRepository.findById(id).orElse(null)

    fun create(fullName: String, email: String?, phone: String?, passwordPlain: String, isActive: Boolean): EmployeeEntity {
        val name = fullName.trim()
        require(name.isNotEmpty()) { "Укажите ФИО" }
        val pwd = passwordPlain.trim()
        require(pwd.isNotEmpty()) { "Укажите пароль" }
        val mail = email?.trim()?.takeIf { it.isNotEmpty() }
        if (mail != null && employeeRepository.existsByEmail(mail)) {
            throw IllegalArgumentException("Email уже занят")
        }
        val id = UUID.randomUUID()
        val entity = EmployeeEntity(
            id = id,
            fullName = name,
            email = mail,
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            passwordHash = pwd,
            isActive = isActive,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        employeeRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.fullName)
        return entity
    }

    fun update(
        id: UUID,
        fullName: String,
        email: String?,
        phone: String?,
        passwordPlain: String?,
        isActive: Boolean
    ): EmployeeEntity {
        val existing = employeeRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val name = fullName.trim()
        require(name.isNotEmpty()) { "Укажите ФИО" }
        val mail = email?.trim()?.takeIf { it.isNotEmpty() }
        if (mail != null && mail != existing.email && employeeRepository.existsByEmail(mail)) {
            throw IllegalArgumentException("Email уже занят")
        }
        val newHash = passwordPlain?.trim()?.takeIf { it.isNotEmpty() } ?: existing.passwordHash
        val updated = existing.copy(
            fullName = name,
            email = mail,
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            passwordHash = newHash,
            isActive = isActive,
            updatedAt = Instant.now()
        )
        employeeRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.fullName)
        return updated
    }

    fun delete(id: UUID) {
        val existing = employeeRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        employeeRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.fullName)
    }

    companion object {
        const val TOPIC = "employees"
        const val EVENT_NAME = "employees-change"
    }
}
