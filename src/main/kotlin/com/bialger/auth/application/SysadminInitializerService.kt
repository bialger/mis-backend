package com.bialger.auth.application

import com.bialger.auth.domain.PasswordHasher
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.EmployeeRoleRepository
import com.bialger.domain.core.repository.RoleRepository
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.event.ApplicationStartupEvent
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Creates the built-in sysadmin account on first startup if it does not exist.
 * Login: sysadmin@mis.local / password: sysadmin@mis.local (must change on first login).
 */
@Singleton
open class SysadminInitializerService(
    private val employeeRepository: EmployeeRepository,
    private val roleRepository: RoleRepository,
    private val employeeRoleRepository: EmployeeRoleRepository,
    private val passwordHasher: PasswordHasher
) : ApplicationEventListener<ApplicationStartupEvent> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun onApplicationEvent(event: ApplicationStartupEvent) {
        val login = "sysadmin@mis.local"
        val existing = employeeRepository.findByEmail(login)
        if (existing != null) {
            // Safety net for production: if default password is still in place, force first-login rotation.
            if (!existing.mustChangePassword && passwordHasher.matches(login, existing.passwordHash)) {
                employeeRepository.update(
                    existing.copy(
                        mustChangePassword = true,
                        updatedAt = Instant.now()
                    )
                )
                log.warn("Sysadmin account still has default password; first-login password change was enforced.")
            } else {
                log.debug("Sysadmin account already exists — skipping initialization.")
            }
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
            mustChangePassword = true,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        employeeRepository.save(entity)
        employeeRoleRepository.save(id, sysadminRole.id)
        log.info("Sysadmin account created (email={}). Change the default password after first login.", login)
    }
}
