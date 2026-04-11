package com.bialger.graphql

import com.bialger.domain.scheduling.enums.AppointmentStatus
import com.bialger.graphql.model.AppointmentPageGql
import com.bialger.graphql.model.PatientGql
import com.bialger.graphql.model.PatientPageGql
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class GraphqlQueryResolver(
    private val facade: GraphqlFacadeService
) {

    val patients: DataFetcher<PatientPageGql> = DataFetcher { env ->
        val page = intArg(env, "page", 0)
        val size = intArg(env, "size", 20)
        val search = env.getArgument<String?>("search")
        facade.listPatients(page, size, search)
    }

    val patient: DataFetcher<PatientGql?> = DataFetcher { env ->
        val id = uuidArg(env, "id")
        facade.getPatient(id)
    }

    val appointments: DataFetcher<AppointmentPageGql> = DataFetcher { env ->
        val page = intArg(env, "page", 0)
        val size = intArg(env, "size", 20)
        val patientId = env.getArgument<String?>("patientId")?.let(::toUuid)
        val status = env.getArgument<Any?>("status")?.let(::toAppointmentStatus)
        facade.listAppointments(page, size, patientId, status)
    }

    val appointment: DataFetcher<com.bialger.graphql.model.AppointmentGql?> = DataFetcher { env ->
        val id = uuidArg(env, "id")
        facade.getAppointment(id)
    }

    private fun intArg(env: DataFetchingEnvironment, name: String, defaultValue: Int): Int {
        val value = env.getArgument<Any?>(name) ?: return defaultValue
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            else -> value.toString().toIntOrNull()
                ?: throw IllegalArgumentException("$name must be an integer")
        }
    }

    private fun uuidArg(env: DataFetchingEnvironment, name: String): UUID {
        val raw = env.getArgument<String?>(name) ?: throw IllegalArgumentException("$name is required")
        return toUuid(raw)
    }

    private fun toUuid(raw: String): UUID =
        runCatching { UUID.fromString(raw) }
            .getOrElse { throw IllegalArgumentException("Invalid UUID: $raw") }

    private fun toAppointmentStatus(raw: Any): AppointmentStatus {
        val value = when (raw) {
            is AppointmentStatus -> raw.name
            else -> raw.toString()
        }
        return runCatching { AppointmentStatus.valueOf(value) }
            .getOrElse { throw IllegalArgumentException("Invalid appointment status: $value") }
    }
}
