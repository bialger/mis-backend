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

    fun patientsExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.patientsPayload())

    fun patientDetailExtras(id: UUID): Map<String, Any> {
        val payload = crmShellApplicationService.patientDetailPayload(id)
            ?: return mapOf("crmPageJson" to """{"kind":"patient-detail","error":"not_found"}""")
        return shellExtras(payload)
    }

    fun doctorsExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.doctorsPayload())

    fun scheduleExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.schedulePayload())

    fun appointmentsListExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.appointmentsListPayload())

    fun appointmentDetailExtras(id: UUID): Map<String, Any> {
        val payload = crmShellApplicationService.appointmentDetailPayload(id)
            ?: return mapOf("crmPageJson" to """{"kind":"appointment-detail","error":"not_found"}""")
        return shellExtras(payload)
    }

    fun inventoryExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.inventoryPayload())

    fun settingsExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.settingsPayload())

    fun reportsExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.reportsPayload())

    fun auditExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.auditPayload())

    fun dashboardExtras(): Map<String, Any> = shellExtras(crmShellApplicationService.dashboardPayload())

    fun bootstrapPayload(kind: String, patientId: UUID?, appointmentId: UUID?): Map<String, Any?> =
        crmShellApplicationService.bootstrapPayload(kind, patientId, appointmentId)

    private fun shellExtras(payload: Map<String, Any?>): Map<String, Any> =
        mapOf(
            "crmPageJson" to objectMapper.writeValueAsString(payload)
        )
}
