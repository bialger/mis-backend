package com.bialger.db

import com.bialger.db.entity.EmployeeEntity
import com.bialger.db.repository.EmployeeRepository
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.StringSpec
import java.util.UUID

@MicronautTest(transactional = true)
class EmployeeRepositoryTest(
    private val repository: EmployeeRepository
) : StringSpec({

    "save and findById" {
        val entity = EmployeeEntity(
            id = UUID.randomUUID(),
            fullName = "Иванов Иван Иванович",
            email = "ivanov@test.mis",
            passwordHash = "hashed",
            isActive = true
        )
        repository.save(entity)

        val found = repository.findById(entity.id).orElse(null)
        found.shouldNotBeNull()
        found.fullName shouldBe "Иванов Иван Иванович"
        found.email shouldBe "ivanov@test.mis"
    }

    "findByEmail" {
        val id = UUID.randomUUID()
        repository.save(
            EmployeeEntity(
                id = id,
                fullName = "Петров",
                email = "petrov@test.mis",
                passwordHash = "hash",
                isActive = true
            )
        )

        val found = repository.findByEmail("petrov@test.mis")
        found.shouldNotBeNull()
        found.id shouldBe id
    }

    "existsByEmail returns true for existing email" {
        val id = UUID.randomUUID()
        repository.save(
            EmployeeEntity(id = id, fullName = "Тест", email = "exists@test.mis", passwordHash = "x", isActive = true)
        )
        repository.existsByEmail("exists@test.mis") shouldBe true
    }

    "existsByEmail returns false for non-existing email" {
        repository.existsByEmail("nonexistent@test.mis") shouldBe false
    }
})
