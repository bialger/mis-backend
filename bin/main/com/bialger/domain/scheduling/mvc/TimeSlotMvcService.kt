package com.bialger.domain.scheduling.mvc

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
