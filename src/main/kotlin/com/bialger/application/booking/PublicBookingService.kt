package com.bialger.application.booking

import com.bialger.api.dto.PublicBookingBranchDto
import com.bialger.api.dto.PublicBookingCreateAppointmentDto
import com.bialger.api.dto.PublicBookingCreatedDto
import com.bialger.api.dto.PublicBookingDoctorDto
import com.bialger.api.dto.PublicBookingSlotDto
import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.core.mvc.EmployeeMvcService
import com.bialger.domain.patient.mvc.PatientMvcService
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.enums.AppointmentSource
import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.domain.scheduling.mvc.AppointmentMvcService
import com.bialger.domain.scheduling.repository.AppointmentRepository
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@Singleton
class PublicBookingService(
    private val branchRepository: BranchRepository,
    private val roomRepository: RoomRepository,
    private val employeeMvcService: EmployeeMvcService,
    private val timeSlotRepository: TimeSlotRepository,
    private val appointmentRepository: AppointmentRepository,
    private val patientMvcService: PatientMvcService,
    private val appointmentMvcService: AppointmentMvcService
) {

    fun listBranches(): List<PublicBookingBranchDto> =
        branchRepository.findAllOrdered()
            .filter { it.isActive }
            .map { b ->
                PublicBookingBranchDto(
                    id = b.id.toString(),
                    organizationId = b.organizationId.toString(),
                    name = b.name,
                    startTime = b.startTime.toString(),
                    endTime = b.endTime.toString()
                )
            }

    fun listDoctors(branchId: UUID): List<PublicBookingDoctorDto> {
        ensureBranch(branchId)
        return employeeMvcService.listForOnlineBooking(branchId).map { e ->
            PublicBookingDoctorDto(
                id = e.id.toString(),
                name = e.fullName,
                workStartTime = e.workStartTime?.toString(),
                workEndTime = e.workEndTime?.toString()
            )
        }
    }

    fun listSlots(branchId: UUID, employeeId: UUID, slotDate: LocalDate): List<PublicBookingSlotDto> {
        val branch = ensureBranch(branchId)
        val employee = ensureDoctorForBranch(branchId, employeeId)
        val window = effectiveWindow(branch, employee) ?: return emptyList()
        val (windowStart, windowEnd) = window

        val allEmployeeSlots = timeSlotRepository.findByBranchIdAndSlotDate(branchId, slotDate)
            .filter { it.employeeId == employeeId }
            .filter { overlaps(it.startTime, it.endTime, windowStart, windowEnd) }
            .sortedBy { it.startTime }
        val slotByBounds = allEmployeeSlots.associateBy { it.startTime to it.endTime }
        val blockedSlotIds = blockedSlotIds(allEmployeeSlots.map { it.id })
        val blockedRanges = allEmployeeSlots
            .filter { it.id in blockedSlotIds || !it.isAvailable }
            .map { it.startTime to it.endTime }

        val roomsById = roomRepository.findByIds(allEmployeeSlots.map { it.roomId }.distinct())
            .associateBy({ it.id }, { it.name })
        val defaultRoom = roomRepository.findByBranchIdAndIsActive(branchId, true)
            .sortedBy { it.name }
            .firstOrNull()
            ?.let { it.id to it.name }
            ?: allEmployeeSlots.firstOrNull()?.let { it.roomId to (roomsById[it.roomId] ?: "") }

        val slotMinutes = inferSlotDurationMinutes(allEmployeeSlots)
        val intervals = generateIntervals(windowStart, windowEnd, slotMinutes)
        val zone = ZoneId.systemDefault()
        return intervals.mapNotNull { (startTime, endTime) ->
            val slot = slotByBounds[startTime to endTime]
            val room = when {
                slot != null -> slot.roomId to (roomsById[slot.roomId] ?: "")
                defaultRoom != null -> defaultRoom
                else -> null
            }
            if (room == null) return@mapNotNull null

            val isBlockedByRange = blockedRanges.any { (blockedStart, blockedEnd) ->
                overlaps(startTime, endTime, blockedStart, blockedEnd)
            }
            val free = if (slot != null) {
                slot.isAvailable && slot.id !in blockedSlotIds
            } else {
                !isBlockedByRange
            }
            val start = slotDate.atTime(startTime).atZone(zone).toInstant().toString()
            val end = slotDate.atTime(endTime).atZone(zone).toInstant().toString()
            val realId = slot?.id?.toString()
            PublicBookingSlotDto(
                id = realId ?: syntheticSlotId(branchId, employeeId, slotDate, startTime, endTime),
                timeSlotId = realId,
                branchId = branchId.toString(),
                employeeId = employeeId.toString(),
                roomId = room.first.toString(),
                slotDate = slotDate.toString(),
                startTime = startTime.toString(),
                endTime = endTime.toString(),
                start = start,
                end = end,
                roomName = room.second,
                free = free
            )
        }
    }

    fun createAppointment(dto: PublicBookingCreateAppointmentDto): PublicBookingCreatedDto {
        val fullName = dto.fullName.trim()
        val phone = dto.phone.trim()
        if (fullName.isEmpty() || phone.isEmpty()) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "fullName and phone are required")
        }
        val branch = ensureBranch(dto.branchId)
        val employee = ensureDoctorForBranch(dto.branchId, dto.employeeId)
        val slot = resolveSlotForBooking(dto, branch, employee)

        val patient = patientMvcService.create(
            organizationId = branch.organizationId,
            cardNumber = "WEB-${UUID.randomUUID().toString().replace("-", "").take(12)}",
            fullName = fullName,
            gender = null,
            birthDate = null,
            phone = phone,
            email = null,
            registrationAddress = null,
            residenceAddress = null,
            localityType = null,
            citizenship = null,
            identityDocument = null,
            omsPolicy = null,
            snils = null,
            insuranceOrganization = null,
            contactPerson = null,
            guardian = null,
            profession = null,
            workplace = null
        )

        val appointment = appointmentMvcService.create(
            patientId = patient.id,
            employeeId = dto.employeeId,
            timeSlotId = slot.id,
            branchId = dto.branchId,
            roomId = slot.roomId,
            status = AppointmentStatus.SCHEDULED,
            source = AppointmentSource.ONLINE,
            notes = dto.comment?.trim()?.takeIf { it.isNotEmpty() },
            createdBy = null
        )

        return PublicBookingCreatedDto(
            patientId = patient.id.toString(),
            appointmentId = appointment.id.toString(),
            status = appointment.status.name,
            source = appointment.source.name,
            message = "Запись создана. Мы ждём вас в клинике."
        )
    }

    private fun resolveSlotForBooking(
        dto: PublicBookingCreateAppointmentDto,
        branch: BranchEntity,
        employee: EmployeeEntity
    ): TimeSlotEntity {
        val explicitSlotId = dto.timeSlotId
        if (explicitSlotId != null) {
            val slot = timeSlotRepository.findById(explicitSlotId).orElseThrow {
                HttpStatusException(HttpStatus.BAD_REQUEST, "Time slot not found")
            }
            if (slot.branchId != dto.branchId || slot.employeeId != dto.employeeId) {
                throw HttpStatusException(HttpStatus.BAD_REQUEST, "Slot does not match selected branch/doctor")
            }
            ensureSlotNotBlocked(slot)
            return slot
        }

        val slotDate = dto.slotDate
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "slotDate is required when timeSlotId is missing")
        val startTime = dto.startTime
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "startTime is required when timeSlotId is missing")
        val endTime = dto.endTime
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "endTime is required when timeSlotId is missing")
        if (endTime <= startTime) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid slot time range")
        }

        val window = effectiveWindow(branch, employee)
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "Doctor has no booking window in this branch")
        if (!containsRange(window.first, window.second, startTime, endTime)) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Selected slot is outside doctor/branch work hours")
        }

        val doctorDaySlots = timeSlotRepository.findByBranchIdAndSlotDate(dto.branchId, slotDate)
            .filter { it.employeeId == dto.employeeId }
        val blockedSlotIds = blockedSlotIds(doctorDaySlots.map { it.id })
        val exactSlot = doctorDaySlots.firstOrNull { it.startTime == startTime && it.endTime == endTime }
        if (exactSlot != null) {
            ensureSlotNotBlocked(exactSlot, blockedSlotIds)
            return exactSlot
        }
        val rangeIsBlocked = doctorDaySlots.any { slot ->
            val blocks = !slot.isAvailable || slot.id in blockedSlotIds
            blocks && overlaps(startTime, endTime, slot.startTime, slot.endTime)
        }
        if (rangeIsBlocked) {
            throw HttpStatusException(HttpStatus.CONFLICT, "Slot is already booked")
        }
        val room = roomRepository.findByBranchIdAndIsActive(dto.branchId, true)
            .sortedBy { it.name }
            .firstOrNull()
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "No active room configured for branch")
        val created = TimeSlotEntity(
            id = UUID.randomUUID(),
            employeeId = dto.employeeId,
            roomId = room.id,
            branchId = dto.branchId,
            slotDate = slotDate,
            startTime = startTime,
            endTime = endTime,
            isAvailable = true
        )
        timeSlotRepository.save(created)
        return created
    }

    private fun ensureSlotNotBlocked(slot: TimeSlotEntity, precomputedBlockedIds: Set<UUID>? = null) {
        if (!slot.isAvailable) {
            throw HttpStatusException(HttpStatus.CONFLICT, "Slot is unavailable")
        }
        val blockedIds = precomputedBlockedIds ?: blockedSlotIds(listOf(slot.id))
        if (slot.id in blockedIds) {
            throw HttpStatusException(HttpStatus.CONFLICT, "Slot is already booked")
        }
    }

    private fun ensureBranch(branchId: UUID): BranchEntity =
        branchRepository.findById(branchId).orElseThrow {
            HttpStatusException(HttpStatus.BAD_REQUEST, "Branch not found")
        }

    private fun ensureDoctorForBranch(branchId: UUID, employeeId: UUID): EmployeeEntity =
        employeeMvcService.listForOnlineBooking(branchId).firstOrNull { it.id == employeeId }
            ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "Doctor not found in selected branch")

    private fun blockedSlotIds(slotIds: List<UUID>): Set<UUID> {
        if (slotIds.isEmpty()) return emptySet()
        return appointmentRepository.findAllOrdered()
            .asSequence()
            .filter { it.timeSlotId != null }
            .filter { it.timeSlotId in slotIds }
            .filter { it.status in BLOCKING_STATUSES }
            .mapNotNull { it.timeSlotId }
            .toSet()
    }

    private fun effectiveWindow(branch: BranchEntity, employee: EmployeeEntity): Pair<LocalTime, LocalTime>? {
        val start = maxOf(branch.startTime, employee.workStartTime ?: branch.startTime)
        val end = minOf(branch.endTime, employee.workEndTime ?: branch.endTime)
        return if (end > start) start to end else null
    }

    private fun inferSlotDurationMinutes(existingSlots: List<TimeSlotEntity>): Long {
        val durations = existingSlots
            .map { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
            .filter { it in 5..180 }
        return durations.minOrNull() ?: DEFAULT_SLOT_MINUTES
    }

    private fun generateIntervals(
        windowStart: LocalTime,
        windowEnd: LocalTime,
        slotMinutes: Long
    ): List<Pair<LocalTime, LocalTime>> {
        val result = mutableListOf<Pair<LocalTime, LocalTime>>()
        var cur = windowStart
        while (cur < windowEnd) {
            val next = cur.plusMinutes(slotMinutes)
            if (next > windowEnd) break
            result += cur to next
            cur = next
        }
        return result
    }

    private fun containsRange(
        windowStart: LocalTime,
        windowEnd: LocalTime,
        slotStart: LocalTime,
        slotEnd: LocalTime
    ): Boolean = slotStart >= windowStart && slotEnd <= windowEnd && slotEnd > slotStart

    private fun overlaps(
        startA: LocalTime,
        endA: LocalTime,
        startB: LocalTime,
        endB: LocalTime
    ): Boolean = startA < endB && endA > startB

    private fun syntheticSlotId(
        branchId: UUID,
        employeeId: UUID,
        slotDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): String = "virtual-$branchId-$employeeId-$slotDate-$startTime-$endTime"

    companion object {
        private const val DEFAULT_SLOT_MINUTES = 30L
        private val BLOCKING_STATUSES = setOf(
            AppointmentStatus.SCHEDULED,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.ARRIVED
        )
    }
}
