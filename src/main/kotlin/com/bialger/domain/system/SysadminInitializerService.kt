package com.bialger.domain.system

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeBranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.RoleRepository
import com.bialger.security.PasswordHasher
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.event.ApplicationStartupEvent
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Creates the built-in sysadmin account on first startup if it does not exist.
 * Login: sysadmin / password: sysadmin (change after first login).
 */
@Singleton
open class SysadminInitializerService(
    private val employeeRepository: EmployeeRepository,
    private val roleRepository: RoleRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val employeeBranchRepository: EmployeeBranchRepository,
    private val passwordHasher: PasswordHasher
) : ApplicationEventListener<ApplicationStartupEvent> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun onApplicationEvent(event: ApplicationStartupEvent) {
        val login = "sysadmin"
        if (employeeRepository.existsByEmail(login)) {
            log.debug("Sysadmin account already exists — skipping initialization.")
            return
        }
        val sysadminRole = roleRepository.findByName("SYSADMIN")
        if (sysadminRole == null) {
            log.warn("SYSADMIN role not found in DB — skipping sysadmin initialization. Check Flyway migrations.")
            return
        }
        val id = UUID.randomUUID()
        val entity = EmployeeEntity(
            id = id,
            fullName = "Системный администратор",
            email = login,
            phone = null,
            passwordHash = passwordHasher.hash(login),
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        employeeRepository.save(entity)
        employeeRoleRepository.save(id, sysadminRole.id)
        log.info("Sysadmin account created (email=sysadmin). Change the default password after first login.")
    }
}
