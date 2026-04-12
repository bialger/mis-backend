package com.bialger.domain.core.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@MappedEntity("employee")
data class EmployeeEntity(
    @Id val id: UUID,
    val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val passwordHash: String,
    val isActive: Boolean = true,
    val digitalSignature: ByteArray? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    /** Optional; when null, online booking uses branch hours only. */
    val workStartTime: LocalTime? = null,
    val workEndTime: LocalTime? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EmployeeEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
