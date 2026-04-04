package com.bialger.graphql

import com.bialger.graphql.model.AppointmentGql
import com.bialger.graphql.model.AppointmentPageGql
import com.bialger.graphql.model.BranchGql
import com.bialger.graphql.model.EmployeeGql
import com.bialger.graphql.model.PatientGql
import com.bialger.graphql.model.PaymentGql
import com.bialger.graphql.model.RoomGql
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class GraphqlFieldResolver(
    private val facade: GraphqlFacadeService
) {

    val patientAppointments: DataFetcher<AppointmentPageGql> = DataFetcher { env ->
        val parent = env.sourceAs<PatientGql>()
        val page = intArg(env, "page", 0)
        val size = intArg(env, "size", 20)
        facade.listPatientAppointments(toUuid(parent.id), page, size)
    }

    val appointmentPatient: DataFetcher<PatientGql?> = DataFetcher { env ->
        facade.appointmentPatient(env.sourceAs())
    }

    val appointmentEmployee: DataFetcher<EmployeeGql?> = DataFetcher { env ->
        facade.appointmentEmployee(env.sourceAs())
    }

    val appointmentBranch: DataFetcher<BranchGql?> = DataFetcher { env ->
        facade.appointmentBranch(env.sourceAs())
    }

    val appointmentRoom: DataFetcher<RoomGql?> = DataFetcher { env ->
        facade.appointmentRoom(env.sourceAs())
    }

    val appointmentPayments: DataFetcher<List<PaymentGql>> = DataFetcher { env ->
        facade.appointmentPayments(env.sourceAs())
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

    private fun toUuid(raw: String): UUID =
        runCatching { UUID.fromString(raw) }
            .getOrElse { throw IllegalArgumentException("Invalid UUID: $raw") }

    private inline fun <reified T> DataFetchingEnvironment.sourceAs(): T {
        val value = this.getSource<Any?>()
        return value as? T ?: throw IllegalArgumentException("Invalid GraphQL source type")
    }
}
