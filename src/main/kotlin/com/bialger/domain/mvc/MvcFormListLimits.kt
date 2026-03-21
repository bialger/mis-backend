package com.bialger.domain.mvc

import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.entity.SpecialtyEntity
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.SpecialtyRepository
import com.bialger.domain.patient.entity.PatientEntity
import com.bialger.domain.patient.repository.PatientRepository
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import java.util.UUID

/** Caps for MVC <select> lists so edit pages stay fast on large databases. */
object MvcFormListLimits {
    const val PATIENTS = 400
    const val EMPLOYEES = 300
    const val TIME_SLOTS = 500
    const val APPOINTMENTS_FOR_PAYMENT = 300
}

fun patientDropdown(repo: PatientRepository, ensureId: UUID?): List<PatientEntity> {
    val base = repo.findTopOrdered(MvcFormListLimits.PATIENTS)
    val id = ensureId ?: return base
    if (base.any { it.id == id }) return base
    val extra = repo.findById(id).orElse(null) ?: return base
    return (base + extra).sortedBy { it.fullName }
}

fun employeeDropdown(repo: EmployeeRepository, ensureId: UUID?): List<EmployeeEntity> {
    val base = repo.findTopOrdered(MvcFormListLimits.EMPLOYEES)
    val id = ensureId ?: return base
    if (base.any { it.id == id }) return base
    val extra = repo.findById(id).orElse(null) ?: return base
    return (base + extra).sortedBy { it.fullName }
}

fun specialtyDropdown(repo: SpecialtyRepository, ensureId: UUID?): List<SpecialtyEntity> {
    val base = repo.findAllOrdered()
    val id = ensureId ?: return base
    if (base.any { it.id == id }) return base
    val extra = repo.findById(id).orElse(null) ?: return base
    return (base + extra).sortedBy { it.name }
}

fun timeSlotDropdown(repo: TimeSlotRepository, ensureId: UUID?): List<TimeSlotEntity> {
    val base = repo.findTopOrdered(MvcFormListLimits.TIME_SLOTS)
    val id = ensureId ?: return base
    if (base.any { it.id == id }) return base
    val extra = repo.findById(id).orElse(null) ?: return base
    return (base + extra).sortedWith(
        compareByDescending<TimeSlotEntity> { it.slotDate }.thenByDescending { it.startTime }
    )
}
