package com.bialger.domain.scheduling.mvc

import com.bialger.domain.core.entity.BranchEntity
import com.bialger.domain.core.entity.EmployeeEntity
import com.bialger.domain.core.repository.BranchRepository
import com.bialger.domain.core.repository.EmployeeRepository
import com.bialger.domain.core.repository.RoomRepository
import com.bialger.domain.scheduling.entity.TimeSlotEntity
import com.bialger.domain.scheduling.repository.TimeSlotRepository
import com.bialger.web.DomainMvcEventEmitter
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class TimeSlotListRow(
    val slot: TimeSlotEntity,
    val employeeName: String,
    val roomName: String,
    val branchName: String
)

@Singleton
class TimeSlotMvcService(
    private val timeSlotRepository: TimeSlotRepository,
    private val employeeRepository: EmployeeRepository,
    private val roomRepository: RoomRepository,
    private val branchRepository: BranchRepository,
    private val domainMvcEventEmitter: DomainMvcEventEmitter
) {

    fun listRows(): List<TimeSlotListRow> {
        val slots = timeSlotRepository.findAllOrdered()
        if (slots.isEmpty()) return emptyList()
        val empIds = slots.map { it.employeeId }.distinct()
        val roomIds = slots.map { it.roomId }.distinct()
        val branchIds = slots.map { it.branchId }.distinct()
        val employees =
            if (empIds.isEmpty()) emptyMap()
            else employeeRepository.findByIds(empIds).associate { it.id to it.fullName }
        val rooms =
            if (roomIds.isEmpty()) emptyMap()
            else roomRepository.findByIds(roomIds).associate { it.id to it.name }
        val branches =
            if (branchIds.isEmpty()) emptyMap()
            else branchRepository.findByIds(branchIds).associate { it.id to it.name }
        return slots.map { s ->
            TimeSlotListRow(
                s,
                employees[s.employeeId] ?: s.employeeId.toString(),
                rooms[s.roomId] ?: s.roomId.toString(),
                branches[s.branchId] ?: s.branchId.toString()
            )
        }
    }

    /**
     * Slots for one branch / doctor / day, limited to the intersection of branch and doctor reception hours.
     */
    fun listRowsForOnlineBooking(branchId: UUID, employeeId: UUID, slotDate: LocalDate): List<TimeSlotListRow> {
        val branch = branchRepository.findById(branchId).orElse(null) ?: return emptyList()
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return emptyList()
        if (!employee.isActive) return emptyList()
        val window = effectiveOnlineBookingWindow(branch, employee) ?: return emptyList()
        val (wStart, wEnd) = window
        // Use findByBranchIdAndSlotDate (known-good) then filter by employee — avoids brittle multi-param queries.
        val slots = timeSlotRepository.findByBranchIdAndSlotDate(branchId, slotDate)
            .filter { it.employeeId == employeeId }
            .filter { overlapsWindow(it, wStart, wEnd) }
            .sortedBy { it.startTime }
        if (slots.isEmpty()) return emptyList()
        val roomIds = slots.map { it.roomId }.distinct()
        val roomNames =
            if (roomIds.isEmpty()) emptyMap()
            else roomRepository.findByIds(roomIds).associate { it.id to it.name }
        val branchName = branch.name
        val empName = employee.fullName
        return slots.map { s ->
            TimeSlotListRow(
                s,
                empName,
                roomNames[s.roomId] ?: s.roomId.toString(),
                branchName
            )
        }
    }

    private fun effectiveOnlineBookingWindow(branch: BranchEntity, employee: EmployeeEntity): Pair<LocalTime, LocalTime>? {
        val bs = branch.startTime
        val be = branch.endTime
        if (be <= bs) return null
        val es = employee.workStartTime ?: bs
        val ee = employee.workEndTime ?: be
        val start = maxOf(bs, es)
        val end = minOf(be, ee)
        return if (end > start) start to end else null
    }

    /** Slot overlaps [wStart, wEnd) style window (half-open end avoids dropping 20:00–20:00 edge cases). */
    private fun overlapsWindow(s: TimeSlotEntity, wStart: LocalTime, wEnd: LocalTime): Boolean =
        s.startTime < wEnd && s.endTime > wStart

    fun getById(id: UUID): TimeSlotEntity? = timeSlotRepository.findById(id).orElse(null)

    fun create(
        employeeId: UUID,
        roomId: UUID,
        branchId: UUID,
        slotDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        isAvailable: Boolean
    ): TimeSlotEntity {
        require(employeeRepository.findById(employeeId).isPresent) { "Сотрудник не найден" }
        require(roomRepository.findById(roomId).isPresent) { "Кабинет не найден" }
        require(branchRepository.findById(branchId).isPresent) { "Филиал не найден" }
        require(endTime > startTime) { "Время окончания должно быть позже начала" }
        val id = UUID.randomUUID()
        val entity = TimeSlotEntity(
            id = id,
            employeeId = employeeId,
            roomId = roomId,
            branchId = branchId,
            slotDate = slotDate,
            startTime = startTime,
            endTime = endTime,
            isAvailable = isAvailable
        )
        timeSlotRepository.save(entity)
        domainMvcEventEmitter.notify(TOPIC, "CREATED", id.toString(), slotDate.toString())
        return entity
    }

    fun update(
        id: UUID,
        employeeId: UUID,
        roomId: UUID,
        branchId: UUID,
        slotDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        isAvailable: Boolean
    ): TimeSlotEntity {
        require(employeeRepository.findById(employeeId).isPresent) { "Сотрудник не найден" }
        require(roomRepository.findById(roomId).isPresent) { "Кабинет не найден" }
        require(branchRepository.findById(branchId).isPresent) { "Филиал не найден" }
        require(endTime > startTime) { "Время окончания должно быть позже начала" }
        val existing = timeSlotRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        val updated = existing.copy(
            employeeId = employeeId,
            roomId = roomId,
            branchId = branchId,
            slotDate = slotDate,
            startTime = startTime,
            endTime = endTime,
            isAvailable = isAvailable
        )
        timeSlotRepository.update(updated)
        domainMvcEventEmitter.notify(TOPIC, "UPDATED", id.toString(), slotDate.toString())
        return updated
    }

    fun delete(id: UUID) {
        val existing = timeSlotRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        timeSlotRepository.deleteById(id)
        domainMvcEventEmitter.notify(TOPIC, "DELETED", id.toString(), existing.slotDate.toString())
    }

    companion object {
        const val TOPIC = "time-slots"
        const val EVENT_NAME = "time-slots-change"

        fun parseLocalDate(raw: String): LocalDate {
            val t = raw.trim()
            require(t.isNotEmpty()) { "Укажите дату" }
            return LocalDate.parse(t)
        }

        fun parseLocalTime(raw: String): LocalTime {
            val t = raw.trim()
            require(t.isNotEmpty()) { "Укажите время" }
            return LocalTime.parse(t)
        }

        fun parseIsAvailable(raw: String): Boolean = raw.trim() != "false"
    }
}
