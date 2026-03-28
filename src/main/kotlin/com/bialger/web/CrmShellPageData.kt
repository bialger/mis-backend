package com.bialger.web

import com.bialger.application.shell.CrmShellApplicationService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import java.util.UUID

/**
 * Thymeleaf shell extras: serializes [CrmShellApplicationService] JSON into `crmPageJson` and SSE paths.
 */
@Singleton
class CrmShellPageData(
    private val objectMapper: ObjectMapper,
    private val crmShellApplicationService: CrmShellApplicationService
) {

    fun patientsExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.patientsPayload(),
        ssePath = "/mvc/patients/events",
        sseName = com.bialger.domain.patient.mvc.PatientMvcService.EVENT_NAME
    )

    fun patientDetailExtras(id: UUID): Map<String, Any> {
        val payload = crmShellApplicationService.patientDetailPayload(id)
            ?: return mapOf("crmPageJson" to """{"kind":"patient-detail","error":"not_found"}""")
        return shellExtras(
            payload = payload,
            ssePath = "/mvc/patients/events",
            sseName = com.bialger.domain.patient.mvc.PatientMvcService.EVENT_NAME
        )
    }

    fun doctorsExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.doctorsPayload(),
        ssePath = "/mvc/employees/events",
        sseName = com.bialger.domain.core.mvc.EmployeeMvcService.EVENT_NAME
    )

    fun scheduleExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.schedulePayload(),
        ssePath = "/mvc/appointments/events",
        sseName = com.bialger.domain.scheduling.mvc.AppointmentMvcService.EVENT_NAME
    )

    fun appointmentsListExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.appointmentsListPayload(),
        ssePath = "/mvc/appointments/events",
        sseName = com.bialger.domain.scheduling.mvc.AppointmentMvcService.EVENT_NAME
    )

    fun appointmentDetailExtras(id: UUID): Map<String, Any> {
        val payload = crmShellApplicationService.appointmentDetailPayload(id)
            ?: return mapOf("crmPageJson" to """{"kind":"appointment-detail","error":"not_found"}""")
        return shellExtras(
            payload = payload,
            ssePath = "/mvc/appointments/events",
            sseName = com.bialger.domain.scheduling.mvc.AppointmentMvcService.EVENT_NAME
        )
    }

    fun inventoryExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.inventoryPayload(),
        ssePath = "/mvc/inventory-items/events",
        sseName = com.bialger.domain.inventory.mvc.InventoryItemMvcService.EVENT_NAME
    )

    fun settingsExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.settingsPayload(),
        ssePath = "/mvc/system-settings/events",
        sseName = com.bialger.domain.system.mvc.SystemSettingMvcService.EVENT_NAME
    )

    fun reportsExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.reportsPayload(),
        ssePath = "/mvc/appointments/events",
        sseName = com.bialger.domain.scheduling.mvc.AppointmentMvcService.EVENT_NAME
    )

    fun auditExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.auditPayload(),
        ssePath = "/mvc/appointments/events",
        sseName = com.bialger.domain.scheduling.mvc.AppointmentMvcService.EVENT_NAME
    )

    fun dashboardExtras(): Map<String, Any> = shellExtras(
        payload = crmShellApplicationService.dashboardPayload(),
        ssePath = "/mvc/appointments/events",
        sseName = com.bialger.domain.scheduling.mvc.AppointmentMvcService.EVENT_NAME
    )

    fun bootstrapPayload(kind: String, patientId: UUID?, appointmentId: UUID?): Map<String, Any?> =
        crmShellApplicationService.bootstrapPayload(kind, patientId, appointmentId)

    private fun shellExtras(payload: Map<String, Any?>, ssePath: String, sseName: String): Map<String, Any> =
        mapOf(
            "crmPageJson" to objectMapper.writeValueAsString(payload),
            "sseEventsPath" to ssePath,
            "sseEventName" to sseName
        )
}
