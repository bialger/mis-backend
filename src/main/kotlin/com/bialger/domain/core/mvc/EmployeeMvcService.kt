package com.bialger.domain.core.mvc

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeBranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.EmployeeSpecialtyRepository
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.domain.core.repository.SpecialtyRepository
import com.bialger.security.PasswordHasher
import com.bialger.web.DomainMvcEventEmitter
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID

/**
 * Persists employee passwords as BCrypt hashes in [EmployeeEntity.passwordHash].
 * Syncs employee_specialty, employee_branch, and employee_role junction tables from forms.
 */
@Singleton
open class EmployeeMvcService(
    private val employeeRepository: EmployeeRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter,
    private val passwordHasher: PasswordHasher,
    private val specialtyRepository: SpecialtyRepository,
    private val branchRepository: BranchRepository,
    private val roleRepository: RoleRepository,
    private val employeeSpecialtyRepository: EmployeeSpecialtyRepository,
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val employeeRoleRepository: EmployeeRoleRepository
) {

    @Transactional(readOnly = true)
    open fun listAll(): List<EmployeeEntity> = employeeRepository.findAllOrdered()

    @Transactional(readOnly = true)
    open fun getById(id: UUID): EmployeeEntity? = employeeRepository.findById(id).orElse(null)

    @Transactional(readOnly = true)
    open fun formExtras(employeeId: UUID?): Map<String, Any> {
        val specialties = specialtyRepository.findAllOrdered()
        val branches = branchRepository.findAllOrdered()
        val roles = roleRepository.findAllOrdered()
        if (employeeId == null) {
            return mapOf(
                "specialties" to specialties,
                "branches" to branches,
                "roles" to roles,
                "employeeSpecialtyIds" to emptySet<String>(),
                "employeeBranchIds" to emptySet<String>(),
                "employeeRoleId" to ""
            )
        }
        val roleIdStr = employeeRoleRepository.findByEmployeeId(employeeId).firstOrNull()?.roleId?.toString() ?: ""
        return mapOf(
            "specialties" to specialties,
            "branches" to branches,
            "roles" to roles,
            "employeeSpecialtyIds" to employeeSpecialtyRepository.findByEmployeeId(employeeId)
                .map { it.specialtyId.toString() }.toSet(),
            "employeeBranchIds" to employeeBranchRepository.findByEmployeeId(employeeId)
                .map { it.branchId.toString() }.toSet(),
            "employeeRoleId" to roleIdStr
        )
    }

    @Transactional(readOnly = true)
    open fun detailExtras(id: UUID): Map<String, Any>? {
        val e = getById(id) ?: return null
        val specLinks = employeeSpecialtyRepository.findByEmployeeId(id)
        val branchLinks = employeeBranchRepository.findByEmployeeId(id)
        val roleLinks = employeeRoleRepository.findByEmployeeId(id)
        val specIds = specLinks.map { it.specialtyId }.distinct()
        val branchIds = branchLinks.map { it.branchId }.distinct()
        val roleIds = roleLinks.map { it.roleId }.distinct()
        val specById =
            if (specIds.isEmpty()) emptyMap()
            else specialtyRepository.findByIds(specIds).associateBy { it.id }
        val branchById =
            if (branchIds.isEmpty()) emptyMap()
            else branchRepository.findByIds(branchIds).associateBy { it.id }
        val roleById =
            if (roleIds.isEmpty()) emptyMap()
            else roleRepository.findByIds(roleIds).associateBy { it.id }
        val specialtyNames = specLinks.mapNotNull { specById[it.specialtyId]?.name }
        val branchNames = branchLinks.mapNotNull { branchById[it.branchId]?.name }
        val roleNames = roleLinks.mapNotNull {
            roleById[it.roleId]?.let { r -> r.displayName ?: r.name }
        }
        return mapOf(
            "item" to e,
            "specialtyNames" to specialtyNames,
            "branchNames" to branchNames,
            "roleNames" to roleNames
        )
    }

    @Transactional
    open fun create(
        fullName: String,
        email: String?,
        phone: String?,
        passwordPlain: String,
        isActive: Boolean,
        specialtyIds: List<UUID>,
        branchIds: List<UUID>,
        roleId: UUID
    ): EmployeeEntity {
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
            passwordHash = passwordHasher.hash(pwd),
            isActive = isActive,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        employeeRepository.save(entity)
        syncSpecialties(id, specialtiesForRole(roleId, specialtyIds))
        syncBranches(id, branchIds)
        syncSingleRole(id, roleId)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), entity.fullName)
        return entity
    }

    @Transactional
    open fun update(
        id: UUID,
        fullName: String,
        email: String?,
        phone: String?,
        passwordPlain: String?,
        isActive: Boolean,
        specialtyIds: List<UUID>,
        branchIds: List<UUID>,
        roleId: UUID
    ): EmployeeEntity {
        val existing = employeeRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val name = fullName.trim()
        require(name.isNotEmpty()) { "Укажите ФИО" }
        val mail = email?.trim()?.takeIf { it.isNotEmpty() }
        if (mail != null && mail != existing.email && employeeRepository.existsByEmail(mail)) {
            throw IllegalArgumentException("Email уже занят")
        }
        val newHash = passwordPlain?.trim()?.takeIf { it.isNotEmpty() }?.let { passwordHasher.hash(it) }
            ?: existing.passwordHash
        val updated = existing.copy(
            fullName = name,
            email = mail,
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            passwordHash = newHash,
            isActive = isActive,
            updatedAt = Instant.now()
        )
        employeeRepository.update(updated)
        syncSpecialties(id, specialtiesForRole(roleId, specialtyIds))
        syncBranches(id, branchIds)
        syncSingleRole(id, roleId)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), updated.fullName)
        return updated
    }

    @Transactional
    open fun delete(id: UUID) {
        val existing = employeeRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        employeeSpecialtyRepository.deleteByEmployeeId(id)
        employeeBranchRepository.deleteByEmployeeId(id)
        employeeRoleRepository.deleteByEmployeeId(id)
        employeeRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.fullName)
    }

    private fun syncSpecialties(employeeId: UUID, ids: List<UUID>) {
        employeeSpecialtyRepository.deleteByEmployeeId(employeeId)
        for (sid in ids.distinct()) {
            require(specialtyRepository.findById(sid).isPresent) { "Специализация не найдена" }
            employeeSpecialtyRepository.save(employeeId, sid)
        }
    }

    private fun syncBranches(employeeId: UUID, ids: List<UUID>) {
        employeeBranchRepository.deleteByEmployeeId(employeeId)
        for (bid in ids.distinct()) {
            require(branchRepository.findById(bid).isPresent) { "Филиал не найден" }
            employeeBranchRepository.save(employeeId, bid)
        }
    }

    private fun specialtiesForRole(roleId: UUID, requested: List<UUID>): List<UUID> {
        val role = roleRepository.findById(roleId).orElseThrow { IllegalArgumentException("Роль не найдена") }
        if (role.name in RolesWithoutSpecialty.NAMES) return emptyList()
        return requested.distinct()
    }

    private fun syncSingleRole(employeeId: UUID, roleId: UUID) {
        employeeRoleRepository.deleteByEmployeeId(employeeId)
        require(roleRepository.findById(roleId).isPresent) { "Роль не найдена" }
        employeeRoleRepository.save(employeeId, roleId)
    }

    private object RolesWithoutSpecialty {
        val NAMES = setOf("SYSADMIN", "ADMIN", "NURSE")
    }

    companion object {
        const val TOPIC = "employees"
        const val EVENT_NAME = "employees-change"
    }
}
